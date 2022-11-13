## Caching what's not to like!

Well quite a bit is 'not to like' if you go about it the wrong way.

First off, only cache if you really need to.

The caveat here is that the result from the same input must be the same every time the call is made,
or at least within a finite period of time; where you can live with inaccurate information for a period of time.

For example, make a remote call to a stock quote system:
- For illustration can be minutes/hours behind actual value
- For real trading has to be as upto date as possible! 

### Why to Cache

Typically, caching is for performance (so don't do this prematurely).
Sometimes it's also a cost thing, calling remote services that cost real money per call (why pay for some calculation when you have already called
and have the result).

### Where to Cache

Some options
- Should the service that is supplying the value, cache it; and then it can return that cached value?
- Should the client making the call cache the result?
- Should something in between the client and the service do all the caching?
- Should cached data be shared across multiple nodes?

### What to Cache

When it comes to deciding what to cache, you can decide to cache raw data, or if using HTTP
just employ some type of caching proxy. You get a lot for free, just by setting some reasonable
`max age` directives. You can go further with `last-modified` and `ETAGS`.

### How long to Cache

This comes down to how long you can live with outdated data. Or if you have direct access to
the true value of the data (i.e. the service you control is the only way to modify data).

i.e. CRUD operations, if you control the gateway in and out then you can ensure that any cached data
is always in sync with the true stored value. Take care here, you are really limiting other
processes from updating data.

### Depending on cached data
If your applications 'only work' when access is provided by a cache. i.e. direct calls to your
main service cause it to 'fail due to load' - You are now in a place where you need very strong operating processes
to 'warm up all your caches'. This has to be done before 'opening the flood gates' to real users.

### Data duplication
You now have the same data in one or more caches as well as your main source of truth.

### Data sensitivity and security
You now also may have some sensitive data being stored in a cache (is that cache secure?).

### Data size
How big should this cache be? What is the policy of removing data.

### The biggest downside
Serving stale data and not being able to work out why or where it is being served from.

## Sounds like it's not worth it

Ideally the 'platform/infrastructure team' will have added some type of **caching proxy** like
*squid* in front of your service. So all HTTP GET calls can be cached, if you've employed **ETAGS** and
caching directives in your response - this will stop your service from being called at all.

If you have to cache data inside your application - because no one want to manage a bit of
infrastructure then you can solve that problem within the application.

Hey great idea - get each microservice team to reinvent the wheel and do their own caching solution!
"`<sarcsm/>`"

For example in this little application, I've decided that each node can have
an in-memory cache of results of a finite size for a finite period.

This is a sort of basic caching mechanism, it means that most calls with be made multiple times
across a number of pods (when deployed in Kubernetes) and some of that results may have been
calculated by other nodes. The cache will be finite in size and so sometimes will not be used and
the data stored will only be held for short time.

So is this worth it? Well in true agile mode, try it in a sliding scale.
Do some load tests with jmeter (for example) and see:

- No caching - what's a performance like - under typically and peak loads.
- Basic in memory caching but with a shared cache.

Then go from there. There is also a potential issue relating to response times, do you want
high variability in response times, or a more consistent response time.

So some companies actually add short delays to responses to try and get them all to be within
a finite range. For example in this little project I've now updated the code to ensure responses always
take at least 10 milliseconds. Sometimes this can be contractual, but sometimes you just want 
a consistent speed for interactions. If, for example your responses are measured, and you have to meet
specific criteria based on standard deviations as well as concrete values. It is sometimes
better to increase the time your responses take so that more responses fall within the metrics required.

## The Changes I've made for caching

I've added some configuration:
```
@Configuration
@EnableCaching
public class Boot2CachingConfiguration extends CachingConfigurerSupport {
  @Bean
  @Override
  public CacheManager cacheManager() {
    ConcurrentMapCacheManager cacheManager = new ConcurrentMapCacheManager() {

      @Override
      protected Cache createConcurrentMapCache(final String name) {
        return new ConcurrentMapCache(name,
            CacheBuilder.newBuilder().expireAfterWrite(10, TimeUnit.SECONDS)
                .maximumSize(10000).build().asMap(), false);
      }
    };

    cacheManager.setCacheNames(Arrays.asList("email", "status"));
    return cacheManager;
  }
}
```

Also added some more dependencies in `build.gradle`:
```
dependencies {
    implementation 'org.springframework.boot:spring-boot-starter'
    implementation 'org.springframework.boot:spring-boot-starter-actuator'
    implementation 'org.springframework.boot:spring-boot-starter-web'
    implementation 'org.springframework.boot:spring-boot-starter-validation'
    implementation 'org.springframework.boot:spring-boot-starter-thymeleaf'
    implementation 'org.springframework.boot:spring-boot-starter-cache'
    implementation 'com.hazelcast:hazelcast-all:4.2.5'
    implementation 'org.springdoc:springdoc-openapi-ui:1.6.12'
    testImplementation 'org.springframework.boot:spring-boot-starter-test'
}
```

Added caching directives:
```
/**
 * Email Validator that delegates to a Value Validator.
 * Just provides strong typing and wrapping as a function.
 */
public class EmailValidator implements Function<String, Status> {
  private final Logger logger = LoggerFactory.getLogger(EmailValidator.class);

  private final ValueValidator delegateValidator;

  public EmailValidator(final ValueValidator validator) {
    this.delegateValidator = validator;
  }

  @Override
  @Cacheable(value = "email", key = "#emailAddress")
  public Status apply(String emailAddress) {
    logger.info("Checking email validity of {}", emailAddress);
    return delegateValidator.validate(emailAddress).get();
  }
}
```

And here:
```
/**
 * User Identifier Validator that delegates to a Value Validator.
 */
public class UserIdentifierValidator implements Function<String, Status> {

  private final Logger logger = LoggerFactory.getLogger(UserIdentifierValidator.class);

  private final ValueValidator delegateValidator;

  public UserIdentifierValidator(final ValueValidator validator) {
    this.delegateValidator = validator;
  }

  @Override
  @Cacheable(value = "status", key = "#userIdentifier")
  public Status apply(String userIdentifier) {
    logger.info("Checking status of {}", userIdentifier);
    return delegateValidator.validate(userIdentifier).get();
  }
}
```

Note the use of the `@Cacheable` and the `map` and `key` and how that ties up with `Boot2CachingConfiguration`.

I then did a bit of refactoring and added in some new Functions:
- [Delay.java](src/main/java/com/example/boot2/util/Delay.java)
- [DelayCalculator.java](src/main/java/com/example/boot2/util/DelayCalculator.java)
- [FunctionTimer.java](src/main/java/com/example/boot2/util/FunctionTimer.java)

I've then revisited:
```
@RestController
@ResponseBody
@Validated
public class BasicProcessController {

  private final RequestProcessor requestProcessor;

  public BasicProcessController(UserIdentifierValidator userIdentifierValidator) {
    requestProcessor =
        new RequestProcessor(new Delay<>(10000000, userIdentifierValidator));
  }

  /**
   * Status checks.
   */
  @Operation(summary = "Check the status of the 'user identifier' supplied")
  @GetMapping("/status/{userIdentifier}")
  public ResponseEntity<Status> checkInputValueStatus(
      @Parameter(description = "The 'user identifier' to be checked")
      @PathVariable("userIdentifier") @Size(min = 2, max = 30) String userIdentifier) {

    return requestProcessor.apply(userIdentifier);
  }
}
```

And:
```
@RestController
@ResponseBody
@Validated
public class EmailValidationController {

  private final RequestProcessor requestProcessor;

  public EmailValidationController(EmailValidator emailValidator) {
    requestProcessor = new RequestProcessor(new Delay<>(10000000, emailValidator));
  }

  /**
   * Email address structure validity checks.
   */
  @Operation(summary = "Check the validity of the 'email address' supplied")
  @GetMapping("/email/{emailAddress}")
  public ResponseEntity<Status> checkEmailAddress(
      @Parameter(description = "The 'email address' to be checked") @PathVariable("emailAddress")
      @NotBlank String emailAddress) {

    return requestProcessor.apply(emailAddress);
  }
}
```

#### Why did I add the delay

Mainly to keep the responses the same sort of speed.

I've put the `Delay` code and associated classes in a package called `util` and made them
generic and composable. For me this is one of the biggest advantages of *Functional Programming*.
It is possible to compose functions in various different ways and make refactoring much easier.
