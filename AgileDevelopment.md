## An Agile Approach to Development of an Application

You may or may not like this approach, but it's one I've found works quite well.

But I can't tell you how much the development will cost, or when it will be finished!

But I can tell you, you get value very early on; and you can say 'stop' when you feel
you have enough functionality.

Importantly quality is not sacrificed. Also, things that can be done in parallel by different tram members
can be progressed.

### So lets outline the broad requirement

We need an application that can accept a user identifier and tell us if that user identifier is acceptable.
If it is not acceptable we also want some reasoning as to why. We will only submit user identifier values of
length greater than 1 and less than 31 characters.

A User Identifier is deemed acceptable if it does not have the letter `X` in it (arbitrary) and must not
contain any punctuation characters, and is not blank (i.e just spaces).

The application must be deployable into a Kubernetes cluster.

Note, I've not added any requirements around performance, logging formats or outputs, nor metric/throughput monitoring.
These things can be added later; as stories There's little doubt in production they will be needed.
But we don't need to think about that yet.

Don't slip into 'waterfall mode'. This requirement (while minimal) is sufficient to get going with. Now take
end-user/client/QA/DevOps 'feedback' (sometimes criticism) in a positive manner!

It just means they have more stories for you to implement, there's a temptation to 'feel' this is in someway
a personal attack on your inability to read their minds. Put that to one side, take a deep breath and go for a walk.

So this is initially just three stories:
- The basic application that controls size of incoming user-identifiers (story 1)
- The actual business logic around what qualifies as an acceptable identifier (story 2)
- The packaging and deployment into a Kubernetes cluster (story 3)

If we have a 'team'; all of these stories can start at the same time and be progressed. The team would just
split that work up and coordinate between themselves - pair and mob programming/testing as they go.

### Where to start on the basic application?

Well lets write a test first, this won't be the full and only test, but it's the bare minimum before we can
implement anything or deploy anything anywhere.

So `story 1` just deals with the first paragraph (i.e. basic preconditions and no business logic.)

#### Writing the first basic test

See [The Spring Boot Test](src/test/java/com/example/boot2/Boot2ApplicationTests.java), here's the core of it below.
```
...
@SpringBootTest
@AutoConfigureMockMvc
class Boot2ApplicationTests {

  @Autowired
  ApplicationContext applicationContext;

  @Test
  void contextLoads() {
    assertNotNull(applicationContext);
  }

  @Test
  void testGetStatusOfUser(@Autowired MockMvc mvc) throws Exception {
    mvc.perform(get("/status/SteveLimb")).andExpect(status().isOk());
  }

  @Test
  void testGetStatusNotFound(@Autowired MockMvc mvc) throws Exception {
    mvc.perform(get("/status")).andExpect(status().is(404));
  }

  @Test
  void testGetStatusBadUserIdentifierLengthTooShort(@Autowired MockMvc mvc) throws Exception {
    mvc.perform(get("/status/s")).andExpect(status().is(412));
  }

  @Test
  void testGetStatusBadUserIdentifierLengthTooLong(@Autowired MockMvc mvc) throws Exception {
    mvc.perform(get("/status/s123456789012345678901234567890")).andExpect(status().is(412));
  }
}
```
I've taken the initiative/liberty of defining the URl `context` as **'/status'** and some appropriate response codes.
These are not in any spec, if they are not what the user expects (maybe they would like `BAD_REQUEST` rather than `PRECONDITION_FAILED`) - that's a new story.

Don't get bogged down asking anyone about this, just crack on. Those in the team that have a strong opinion can all argue about it
between themselves for a few days, do not become paralysed or slowed down - just keep driving forward.

Obviously - these tests will all fail, we've not implemented a controller yet!

I did have to modify the `build.gradle` to include:
- `implementation 'org.springframework.boot:spring-boot-starter-web'`
- `implementation 'org.springframework.boot:spring-boot-starter-validation'`

These are needed for the validation and also the `@AutoConfigureMockMvc` used in the tests.

#### Now lets just do the bare minimum to get those test to pass

To do this, I've added a simple `domain` class (actually a record).
[Status.java](src/main/java/com/example/boot2/domain/Status.java) as below:
```
package com.example.boot2.domain;

import java.util.Optional;

public record Status(boolean acceptable, Optional<String> reasonUnacceptable) {

}
```

Then just used that with a simple controller:
```
@RestController
@ResponseBody
@Validated
public class BasicProcessController {

  private final Supplier<Status> acceptable =
      () -> new Status(true, Optional.empty());

  private final BiFunction<HttpStatus, Supplier<Status>, ResponseEntity<Status>> response =
      (statusCode, supplier) -> ResponseEntity.status(statusCode).body(supplier.get());

  @GetMapping("/status/{userIdentifier}")
  public ResponseEntity<Status> checkInputValueStatus(
      @PathVariable("userIdentifier") @Size(min = 2, max = 30) String userIdentifier) {
    return response.apply(HttpStatus.OK, acceptable);
  }
}
```

So that should do it; right? No business logic yet, but in terms of minimal functionality and those preconditions - we're done.

Try the tests again, two still fail! This is because the validation using `@Size` causes a `javax.validation.ConstraintViolationException`.

What we really want is the HTTP code for PRECONDITION_FAILURE here.
So now we need to work with the Spring framework to add in some `Aspect Programming`. Spring enables us to catch the exception via
`AOP` and modify the response.

```
...
@ControllerAdvice(basePackageClasses = BasicProcessController.class)
public class BasicControllerAdvice extends ResponseEntityExceptionHandler {

  @ResponseBody
  @ExceptionHandler(ConstraintViolationException.class)
  public ResponseEntity<Status> handlerControllerException(HttpServletRequest request, Throwable th) {
    return ResponseEntity
        .status(HttpStatus.PRECONDITION_FAILED)
        .body(new Status(false, Optional.of(th.getMessage())));
  }
}
```

#### That's it

Now all our tests pass and if just run up `Boot2Application` and use a browser/curl I can test it out:
```
curl http://localhost:8080/status/s123456789012345678901234567890

# I get this response

{"acceptable":false,"reasonUnacceptable":"checkInputValueStatus.userIdentifier: size must be between 2 and 30"}
```

Here's the important part, this application can now be deployed (if part of the team was working on `story 3`).
The application will respond with everything is acceptable, but it allows QA and clients of the service to start integrating their services.

See later for the `helm` stuff.

So QA can start writing and testing their full set of tests for the 'happy path' and also
get lots of failing tests ready with a real service running.

#### `Story 1` is complete on to the next.
This meets the needs for the first paragraph.
We can work on the next story, that story is the business logic. If we had a team that could be done concurrently.

Just to recap the business logic is:
"A User Identifier is deemed acceptable if it does not have the letter `X` in it (arbitrary) and must not
contain any punctuation characters, and is not blank (i.e just spaces)."

#### `Story 2` - the 'business validation'

OK, it's a bit simple and basic, but let's wrap this up via an interface and have
a service implement it. We can then configure a simple test service that just lets everything
pass and a real production service that does the actual logic.

Obviously; the logic is so simple, but we need to focus on design and isolation of these elements being developed.

Here is the UserIdentifierValidator (interface):
```
...
public interface UserIdentifierValidator {

  Supplier<Status> validate(String userIdentifier);
}
```

Here is the test implementation (you could just mock this - but I don't like mocks much):
TestUserIdentifierValidator, note that this is in the `test` part of the project. But I'll revisit this later.

```
...
@Service
@ConditionalOnProperty(name = "run.system", havingValue = "stub")
public class TestUserIdentifierValidator implements UserIdentifierValidator {

  @Override
  public Supplier<Status> validate(String userIdentifier) {
    return () -> new Status(true, Optional.empty());
  }
}
```

I've used the `@ConditionalOnProperty` and a property called `run.system`, this set up
via the `application.properties`, `application-dev.properties` and the profile `dev`.

I can move on to develop the actual implementation. I'll start off with it all in the
class `ProductionUserIdentifierValidator`. I'll write some tests, then I'll refactor it
and check all the tests still pass.

So here are the tests for ProductionUserIdentifierValidator:
```
...
class UserIdentifierValidatorTest {

  private UserIdentifierValidator underTest = new ProductionUserIdentifierValidator();

  private Consumer<Supplier<Status>> assertFailsBusinessLogic = supplier -> {
    var result = supplier.get();
    assertFalse(result.acceptable());
    assertTrue(result.reasonUnacceptable().isPresent());
    assertEquals("Fails Business Logic Check", result.reasonUnacceptable().get());
  };

  @ParameterizedTest
  @CsvSource({"Steve", "StephenLimb", "Stephen John Limb"})
  void testAcceptableContent(String toBeValidated) {
    var result = underTest.validate(toBeValidated).get();
    assertTrue(result.acceptable());
    assertTrue(result.reasonUnacceptable().isEmpty());
  }

  @ParameterizedTest
  @CsvSource({"SteveX", "Stephen X Limb"})
  void testXContent(String toBeValidated) {
    assertFailsBusinessLogic.accept(underTest.validate(toBeValidated));
  }

  @ParameterizedTest
  @CsvSource({"StephenLimb!", "@StephenLimb", "@", "!", "{"})
  void testPunctuatedContent(String toBeValidated) {
    assertFailsBusinessLogic.accept(underTest.validate(toBeValidated));
  }

  @ParameterizedTest
  @NullSource  // pass a null value
  @ValueSource(strings = {"", " ", "    "})
  void testNullAndBlankContent(String toBeValidated) {
    assertFailsBusinessLogic.accept(underTest.validate(toBeValidated));
  }
}
```

Some of these test pass and some fail, I've used parameterised tests here.
Also, I've not used any `Spring` stuff or contexts or anything like that.

I've gathered common test code assertions into a simple `Consumer` - so even the tests are DRY.
Remember even test code needs maintenance and design. You should view test cases more as an example of how to
use the software you're developing.

The use of just pure Junit tests without any Spring is quick and simple - but only if
you minimise `@Autowiring` and inject dependencies via constructors. This enables you to
create your own `new` Java objects and use them. This does speed up your tests and keeps them
less `Spring` (a good thing in my opinion). Running big complex `Spring context` with all that reflection code
with lots of tests will really slow your test runs down.

#### Next task it to implement the real functionality and get the tests to pass

So here's the initial implementation (needs to be refactored though). But passes all the tests.

In fact, they didn't pass first time. I got the regular expression with `\\p{Punct}` in wrong. But my
tests detected that!

```
...
@Service
@ConditionalOnProperty(name = "run.system", havingValue = "prd")
public class ProductionUserIdentifierValidator implements UserIdentifierValidator {

  @Override
  public Supplier<Status> validate(final String userIdentifier) {
    var valid = isValid(userIdentifier);
    if(valid) {
      return () -> new Status(true, Optional.empty());
    }
    return () -> new Status(false, Optional.of("Fails Business Logic Check"));
   }

  private boolean isValid(String userIdentifier) {
    if(userIdentifier == null || userIdentifier.isBlank()) {
      return false;
    }
    if(userIdentifier.contains("X")) {
      return false;
    }
    if(Pattern.matches("(.*)[\\p{Punct}](.*)", userIdentifier)) {
      return false;
    }
    return true;
  }
}
```

You may ask; "why refactor this?". It's just messy, lets make it more functional.

But this is where 'TDD' drives the implementation to be made tighter, but we've got something
almost working now. We just need to plug the business logic into the controller.

So here is that controller, now revisited.

```
@RestController
@ResponseBody
@Validated
public class BasicProcessController {

  private final UserIdentifierValidator userIdentifierValidator;

  private final BiFunction<HttpStatus, Supplier<Status>, ResponseEntity<Status>> response =
      (statusCode, supplier) -> ResponseEntity.status(statusCode).body(supplier.get());

  public BasicProcessController(UserIdentifierValidator userIdentifierValidator) {
    this.userIdentifierValidator = userIdentifierValidator;
  }

  @GetMapping("/status/{userIdentifier}")
  public ResponseEntity<Status> checkInputValueStatus(
      @PathVariable("userIdentifier") @Size(min = 2, max = 30) String userIdentifier) {
    return response.apply(HttpStatus.OK, userIdentifierValidator.validate(userIdentifier));
  }
}
```

The controller uses whatever `UserIdentifierValidator` is available from the Spring context.
For testing that will just be our basic `TestUserIdentifierValidator` but when we run the application up
in production (none-test) mode it will use the `ProductionUserIdentifierValidator`.

So this is an important point, keep unit tests simple and isolated, keep the Spring controller/application
stuff just using stubs. You can integrate the whole together later. We are really just wanting to test the Spring/Controller
and the entry into the application (that's all).

If we run up our SpringBoot application via our IDE, we can check it all works.
```
curl http://localhost:8080/status/!Check!

# I get this response

{"acceptable":false,"reasonUnacceptable":"Fails Business Logic Check"}
```

#### Refactoring ProductionUserIdentifierValidator

Now I plan to make this more functional and use `Predicates`, but I may get carried away (which is 
sometimes good and sometimes a waste of time). But let's see where this takes us.

So here's the first go at refactoring in a more functional way.
```
@Service
@ConditionalOnProperty(name = "run.system", havingValue = "prd")
public class ProductionUserIdentifierValidator implements UserIdentifierValidator {

  private final Predicate<String> hasValue =
      userIdentifier -> userIdentifier != null && !userIdentifier.isBlank();

  private final Predicate<String> doesNotContainX =
      userIdentifier -> !userIdentifier.contains("X");

  private final Predicate<String> doesNotContainPunctuation =
      userIdentifier -> !Pattern.matches("(.*)[\\p{Punct}](.*)", userIdentifier);

  private final Predicate<String> rules =
      hasValue.and(doesNotContainX).and(doesNotContainPunctuation);

  private final Supplier<Status> valid =
      () -> new Status(true, Optional.empty());

  private final Supplier<Status> invalid =
      () -> new Status(false, Optional.of("Fails Business Logic Check"));

  @Override
  public Supplier<Status> validate(final String userIdentifier) {

    return Optional.ofNullable(userIdentifier)
        .stream()
        .filter(rules)
        .findAny()
        .map(id -> valid)
        .orElse(invalid);
  }
}
```

The 'if statements' and conditional returns have now been redefined as Predicate/Supplier.

Some people may say that the text `"Fails Business Logic Check"` and rule `"(.*)[\\p{Punct}](.*)"` should
be pulled out to static constants, or maybe pulled in from `property files`. There's currently no need for this,
they are well-defined in the context of the `Predicate` and `Supplier`.

This gives me the idea that I could pull those `Predicates` out and use a 
constructor argument to take a predicate in. This code would then be more `SOLID`
and only focus on the running of a `Predicate` and mapping to a Status.

This would mean I could use the same validator for both Test and Production use
but with different rules. See I told you I might get carried away refactoring.

#### The second refactoring
I've removed the `TestUserIdentifierValidator` and the `ProductionUserIdentifierValidator` and
changed `UserIdentifierValidator` from an **interface** in to a **class** with all the implementation in.

But I've also added a configuration class (now there's a bit here I don't like, there is
now some stub code in the main part of the project).

The configuration:
```
@Configuration
public class ValidatorConfiguration {

  @Bean
  @ConditionalOnProperty(name = "run.system", havingValue = "stub")
  UserIdentifierValidator stubValidator() {
    return new UserIdentifierValidator(userIdentifier -> true);
  }

  @Bean
  @ConditionalOnProperty(name = "run.system", havingValue = "prd")
  UserIdentifierValidator productionValidator() {

    final Predicate<String> hasValue =
        userIdentifier -> userIdentifier != null && !userIdentifier.isBlank();

    final Predicate<String> doesNotContainX =
        userIdentifier -> !userIdentifier.contains("X");

    final Predicate<String> doesNotContainPunctuation =
        userIdentifier -> !Pattern.matches("(.*)[\\p{Punct}](.*)", userIdentifier);

    final Predicate<String> rules =
        hasValue.and(doesNotContainX).and(doesNotContainPunctuation);

    return new UserIdentifierValidator(rules);
  }
}
```

The UserIdentifierValidator class:
```
public final class UserIdentifierValidator {
  private final Predicate<String> acceptableRule;

  private final Supplier<Status> valid =
      () -> new Status(true, Optional.empty());

  private final Supplier<Status> invalid =
      () -> new Status(false, Optional.of("Fails Business Logic Check"));

  public UserIdentifierValidator(Predicate<String> acceptableRule) {
    this.acceptableRule = acceptableRule;
  }

  public Supplier<Status> validate(final String userIdentifier) {

    return Optional.ofNullable(userIdentifier)
        .stream()
        .filter(acceptableRule)
        .findAny()
        .map(id -> valid)
        .orElse(invalid);
  }
}
```

## Summary
You can argue the refactoring as gone a bit too far (or not far enough!), but you can see the progression in agile development.
At each stage you have something runnable, this activity (including writing this blurb) took a few hours.

I have separated out all the logic concerns here, the `UserIdentifierValidator` just runs a `Predicate` and delegates
the mapping of the result to one of two `Suppliers`
(even these could have been passed in - if we really wanted to separate those concerns even more).

The `ValidatorConfiguration` is only concerned with creating the right configuration of the `UserIdentifierValidator`.

The thing I like about functional coding and part of Spring, is that now I have a validator that can be configured,
i.e. it is open for extension but does not need modifying itself. In fact; it is now used in two distinct ways.

The Spring `@Configuration` is also good in the fact it is a 'Stereotype' for 'config', so that's quite explicit.

If you notice, I've done all these refactoring changes, all the tests still pass, and importantly I've not had to alter
the 'Controller' at all.

Also, each `rule` has been defined as a `Predicate` and then 'hooked' together. So people might say write a unit test for each of those.
This is where I would argue about the word unit test, some people say it is a test of a class/function.
I'd say it is a unit of functionality.

But if I look at the tests I have, and the code coverage; I get (100%). I'd say I've catered for all conditions here.

Which brings me to the final point, with Agile and TDD done in the way described here, you develop the smallest amount of
code and the minimal number of tests needed to meet the requirement.

Indeed, it was not necessary to refactor and make it more functional, the tests passed and the functionality worked.
But I'd argue it's worth it in the long run, adding a new rule is easy, adding a different configuration based on a property is also easy.
There are no mocks involved, with Java `Functions` it much easier to just plug in a simple function if
the classes defined can be constructed with dependencies being passed in.

I'm now in two minds about separating the `valid` and `invalid` `Suppliers` and pull those into the configuration.
I might discuss this with a team members and see what they think. Doing this would also feel more consistent and
symmetric.

## `Story 3` Packaging and Deployment into Kubernetes

I've created a separate [page on docker and helm](Dockerizing.md); it shows what additional
non-functional changes are needed to take this application and deploy it as a container in a pod in
Kubernetes.

But we do have a SpringBoot Application that meets the basic needs, we just need to package it now.





