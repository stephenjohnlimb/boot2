## Support for Swagger (OpenAPI) documentation

The end-users of our service were surprised there was no documentation on how to use the service.

Now you could react in a defensive (or offended manner) here, but in an Agile world it's only now
they've stated they want to pay for it. So this functionality must have some value for them, now we can crack on
and do it. If we'd have developed it before - maybe that time and effort would have been wasted.

After all; the requirement started out as a single URL context of `/status/{userIdentifier}`, hardly
needs any documentation. But now we've added the `/email/{emailAddress}` URL context; maybe some
documentation is a good idea.

### Adding in Swagger 3 OpenAPI support

Now the first question you need to answer is; are you a 'bottom up' or a 'top down' developer.

As I'm a 'code monkey' - I'm a bottom up developer. I'll define all my documentation via code
and have it generated as documentation using `org.springdoc:springdoc-openapi-ui`.

#### The test
Firstly we need to ensure we have a test to access the url for the documents.
```
@SpringBootTest
@AutoConfigureMockMvc
class Boot2OpenAPITests {

  @Test
  void testGetOpenAPIDocumentationPage(@Autowired MockMvc mvc) throws Exception {
    mvc.perform(get("/api-docs")).andExpect(status().isOk());
  }

  @Test
  void testGetOpenAPIDocumentationYAMLPage(@Autowired MockMvc mvc) throws Exception {
    mvc.perform(get("/api-docs.yaml")).andExpect(status().isOk());
  }

  @Test
  void testGetOpenAPIEndUserDocumentation(@Autowired MockMvc mvc) throws Exception {
    mvc.perform(get("/api-ui.html"))
        .andExpect(status().is(302))
        .andExpect(redirectedUrl("/swagger-ui/index.html"));
  }
}
```

I plan to map the specific URLS that come out of the box to my own end points (so that it is
consistent with other projects).

So the above tests currently fail - because I've not added configuration yet.

#### The basic changes
First lets add in some gradle configuration (`springdoc-openapi-ui`) to `build.gradle`.
```
...
dependencies {
    implementation 'org.springframework.boot:spring-boot-starter'
    implementation 'org.springframework.boot:spring-boot-starter-actuator'
    implementation 'org.springframework.boot:spring-boot-starter-web'
    implementation 'org.springframework.boot:spring-boot-starter-validation'
    implementation 'org.springdoc:springdoc-openapi-ui:1.6.12'
    testImplementation 'org.springframework.boot:spring-boot-starter-test'
}
...
```
Now lets map those bespoke URLS in application.properties:
```
springdoc.api-docs.path=/api-docs
springdoc.swagger-ui.path=/api-ui.html
springdoc.swagger-ui.operationsSorter=method
```

We can run our tests again they should now pass! They do.

#### Manual checking

You can now use your browser and go to `http://localhost:8080/api-ui.html` and try those
methods out.

Also, if you access `http://localhost:8080/api-docs.yaml` you'll get the following:
```
openapi: 3.0.1
info:
  title: OpenAPI definition
  version: v0
servers:
- url: http://localhost:8080
  description: Generated server url
paths:
  /status/{userIdentifier}:
    get:
      tags:
      - basic-process-controller
      operationId: checkInputValueStatus
      parameters:
      - name: userIdentifier
        in: path
        required: true
        schema:
          maxLength: 30
          minLength: 2
          type: string
      responses:
        "200":
          description: OK
          content:
            '*/*':
              schema:
                $ref: '#/components/schemas/Status'
  /email/{emailAddress}:
    get:
      tags:
      - email-validation-controller
      operationId: checkEmailAddress
      parameters:
      - name: emailAddress
        in: path
        required: true
        schema:
          type: string
      responses:
        "200":
          description: OK
          content:
            '*/*':
              schema:
                $ref: '#/components/schemas/Status'
components:
  schemas:
    Status:
      type: object
      properties:
        acceptable:
          type: boolean
        reasonUnacceptable:
          type: string
```

So for very little work, we've met the basic needs of the requirement!

Check it in, job done?

### Is there more we could do?
Well yes, we can improve on the documentation a bit. But first things first, get that code in
and check it actually runs up in a deployed environment (like K8S).

So lets bump our, Application version in [build.gradle](build.gradle) and [Chart.yaml](src/main/helm/boot2/Chart.yaml).
Do a local build and deploy via helm and make sure those URLS all still work OK (manually).

```
helm list
# The output
NAME            NAMESPACE       REVISION        UPDATED                                 STATUS          CHART           APP VERSION
boot2           default         1               2022-10-31 14:58:09.672714 +0000 UTC    deployed        boot2-1.0.1     1.0.4      
my-release      default         1               2022-06-30 19:27:51.07704 +0100 BST     deployed        kafka-18.0.0    3.2.0

./gradlew jib; helm upgrade boot2 ./src/main/helm/boot2

helm list                                                  
NAME            NAMESPACE       REVISION        UPDATED                                 STATUS          CHART           APP VERSION
boot2           default         2               2022-11-01 10:23:26.665446 +0000 UTC    deployed        boot2-1.0.1     1.0.5      
my-release      default         1               2022-06-30 19:27:51.07704 +0100 BST     deployed        kafka-18.0.0    3.2.0

```

Now we can check on the Kubernetes service IP/URLS.
```
curl http://192.168.64.95/api-docs

# The output
{"openapi":"3.0.1","info":{"title":"OpenAPI definition","version":"v0"},"servers":[{"url":"http://192.168.64.95","description":"Generated server url"}],"paths":{"/status/{userIdentifier}":{"get":{"tags":["basic-process-controller"],"operationId":"checkInputValueStatus","parameters":[{"name":"userIdentifier","in":"path","required":true,"schema":{"maxLength":30,"minLength":2,"type":"string"}}],"responses":{"200":{"description":"OK","content":{"*/*":{"schema":{"$ref":"#/components/schemas/Status"}}}}}}},"/email/{emailAddress}":{"get":{"tags":["email-validation-controller"],"operationId":"checkEmailAddress","parameters":[{"name":"emailAddress","in":"path","required":true,"schema":{"type":"string"}}],"responses":{"200":{"description":"OK","content":{"*/*":{"schema":{"$ref":"#/components/schemas/Status"}}}}}}}},"components":{"schemas":{"Status":{"type":"object","properties":{"acceptable":{"type":"boolean"},"reasonUnacceptable":{"type":"string"}}}}}}

curl http://192.168.64.95/api-docs

# The output
openapi: 3.0.1
info:
  title: OpenAPI definition
  version: v0
servers:
- url: http://192.168.64.95
  description: Generated server url
paths:
  /status/{userIdentifier}:
    get:
      tags:
      - basic-process-controller
      operationId: checkInputValueStatus
      parameters:
      - name: userIdentifier
        in: path
        required: true
        schema:
          maxLength: 30
          minLength: 2
          type: string
      responses:
        "200":
          description: OK
          content:
            '*/*':
              schema:
                $ref: '#/components/schemas/Status'
  /email/{emailAddress}:
    get:
      tags:
      - email-validation-controller
      operationId: checkEmailAddress
      parameters:
      - name: emailAddress
        in: path
        required: true
        schema:
          type: string
      responses:
        "200":
          description: OK
          content:
            '*/*':
              schema:
                $ref: '#/components/schemas/Status'
components:
  schemas:
    Status:
      type: object
      properties:
        acceptable:
          type: boolean
        reasonUnacceptable:
          type: string
```

The using a browser, you can go to `http://192.168.64.95/api-ui.html` and interact with the side.

So we're good to 'check-in' our code and next we can refine the documentation - if the team thinks this is
worthwhile.

How long this take up to this point? About an hour - including making tea and writing these pages.

### Improving the documentation
If you recall in `BasicControllerAdvice` we handled the `ConstraintViolationException` which is
throws when JSR303 validation fails. We responded with a `412` precondition failure.

#### The 'Advice'

But the documentation does not currently show this. If we now augment `BasicControllerAdvice` as follows:
```
@ControllerAdvice
public class BasicControllerAdvice extends ResponseEntityExceptionHandler {

  /**
   * Deals with an exception when any of the methods on BasicProcessController are called.
   */
  @ResponseBody
  @ExceptionHandler(ConstraintViolationException.class)
  @ResponseStatus(HttpStatus.PRECONDITION_FAILED)
  public ResponseEntity<Status> handlerControllerException(HttpServletRequest request,
                                                           Throwable th) {
    return ResponseEntity
        .status(HttpStatus.PRECONDITION_FAILED)
        .body(new Status(false, Optional.of(th.getMessage())));
  }
}
```

The documentation also gets updates. This is OK, but if I'm honest having to use
`HttpStatus.PRECONDITION_FAILED` in two places is a bit bad (not very DRY).

#### The main Controllers
I've also added some Open API documentation to the controller services:
```
...
  /**
   * Status checks.
   */
  @Operation(summary = "Check the status of the 'user identifier' supplied")
  @GetMapping("/status/{userIdentifier}")
  public ResponseEntity<Status> checkInputValueStatus(
      @Parameter(description = "The 'user identifier' to be checked")
      @PathVariable("userIdentifier") @Size(min = 2, max = 30) String userIdentifier) {

    logger.info("Checking status of {}", userIdentifier);
    return response.apply(HttpStatus.OK, userIdentifierValidator.apply(userIdentifier));
  }
...
```

And:
```
...
  /**
   * Email address structure validity checks.
   */
  @Operation(summary = "Check the validity of the 'email address' supplied")
  @GetMapping("/email/{emailAddress}")
  public ResponseEntity<Status> checkEmailAddress(
      @Parameter(description = "The 'email address' to be checked")
      @PathVariable("emailAddress") @NotBlank String emailAddress) {

    logger.info("Checking email validity of {}", emailAddress);
    return response.apply(HttpStatus.OK, emailValidator.apply(emailAddress));
  }
```

If I'm honest - I find this all a bit 'busy' - there's more '@' than I'd like. But as this
is the entry point into the application service and does not contain any other functionality;
I suppose this is OK.
#### A new Configuration

```
Configuration
public class OpenApiConfiguration {

  /**
   * Create an OpenAPI bean with all the relevant details in.
   * This can then be displayed in the generated documentation.
   */
  @Bean
  public OpenAPI customOpenApi(@Autowired BuildProperties buildProperties,
                               @Value("${apiTitle}") String apiTitle,
                               @Value("${apiDescription}") String apiDescription,
                               @Value("${apiContactName}") String apiContactName,
                               @Value("${apiContactEmail}") String apiContactEmail) {
    return new OpenAPI()
        .components(new Components())
        .info(new Info()
            .title(apiTitle).description(apiDescription).version(buildProperties.getVersion())
            .contact(new Contact().name(apiContactName).email(apiContactEmail)));
  }
}
```

I had to update `build.gradle` to get the `BuildProperties` injected. I wanted this; so I
could use the same version from the `build.gradle`. Now you could argue that the version
of the API should be different from the version of the application built.

But as we already have a helm chart version, and application version; I did not really
want to introduce an API version. But if you were developing a real API - you'd probably
have to.

```
springBoot {
    buildInfo()
}
```

Also modify `application.properties` with the following:
```
apiTitle=Validations API
apiDescription=API for basic validations
apiContactName=Steve Limb
apiContactEmail=stephenjohnlimb@gmail.com
```

Those properties are now injected as `@Values`.

#### A few other minor changes

I wanted a specific error page to be used for URL's that this application
is **not** serving. So I added a bespoke [error.html](src/main/resources/templates/error.html) and
updated `build.gradle` to include `spring-boot-starter-thymeleaf`:
```
dependencies {
    implementation 'org.springframework.boot:spring-boot-starter'
    implementation 'org.springframework.boot:spring-boot-starter-actuator'
    implementation 'org.springframework.boot:spring-boot-starter-web'
    implementation 'org.springframework.boot:spring-boot-starter-validation'
    implementation 'org.springframework.boot:spring-boot-starter-thymeleaf'
    implementation 'org.springdoc:springdoc-openapi-ui:1.6.12'
    testImplementation 'org.springframework.boot:spring-boot-starter-test'
}
```

Then altered `application.properties` with `server.error.whitelabel.enabled=false`.

I then added an error controller:
```
/**
 * Designed to provide a little more information to the caller in the event of an error.
 */
@Controller
public class ServiceErrorController implements ErrorController {

  private final Logger logger = LoggerFactory.getLogger(ServiceErrorController.class);

  /**
   * Handles any errors and maps through to an error page.
   */
  @RequestMapping("/error")
  public String handleError(HttpServletRequest request) {
    Object status = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
    logger.error("WebService Error {}", status);
    return "error";
  }
}
```

And also the [error.html](src/main/resources/templates/error.html) page:
```
<!DOCTYPE html>
<html>
<body>
<h1>Unable to Service Request</h1>
<h2>Please check following URLS</h2>
<p>Check you have made the correct request.</p>
<a href="/api-docs">Programmatic API Documentation</a>
<br/>
<a href="/api-ui.html">Human API Documentation</a>
</body>
</html>
```

## Summary
That is it for adding OpenAPI/Swagger documentation. So took me about a morning to do,
with a few breaks, a bit of trial and error and writing this page.

But I think this amount of documentation for a REST API is about right.