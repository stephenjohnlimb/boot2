## An Agile Approach to Development of an Application

Now you may or may not like this approach, but it's one I've found works well.

But I can't tell you how much the development will cost, or when it will be finished!

But I can tell you, you get value very early on; and you can say 'stop' when you feel
you have enough functionality.

Importantly quality is not sacrificed.

### So lets outline the broad requirement

We need an application that can accept a user identifier and tell us if that user identifier is acceptable.
If it is not acceptable we also want some reasoning as to why. We will only submit user identifier values of
length greater than 1 and less than 31 characters.

A User Identifier is deemed acceptable if it does not have the letter `X` in it (arbitrary) and must not
contain any punctuation characters.

Note I've not added any requirements around logging formats or outputs, nor metric/throughput monitoring.
These things can be added later as stories, there's little doubt in production they will be needed.
But we don't need to think about that yet.

Don't slip into 'waterfall mode'. But you can't ask "when will 'it' all be finished". If you can't
afford logging and monitoring or are willing to 'risk it' hey that's your choice - I wouldn't - but you might.

### Where to start?

Well lets write a test first, this won't be the full and only test, but it's the bare minimum before we can
implement anything or deploy anything.

So the first `story` just deals with the first paragraph (i.e. basic preconditions and no business logic.)

#### Write a basic test first

See [The Spring Boot Test](src/test/java/com/example/boot2/Boot2ApplicationTests.java).
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
Note that I've taken the 
initiative/liberty of defining the appropriate response codes. These are not in any spec, if they are not what the 
user expects (maybe they would like `BAD_REQUEST` rather than `PRECONDITION_FAILED`) - that's a new story.

Obviously - these tests will all fail, we've not implemented a controller yet!

Here is the first implementation of the controller.
```
...
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

I did have to modify the `build.gradle` to include:
- `implementation 'org.springframework.boot:spring-boot-starter-web'`
- `implementation 'org.springframework.boot:spring-boot-starter-validation'`

These are needed for the validation and also the `@AutoConfigureMockMvc` used in the tests.

#### Now lets just do the bare minimum to get those test to pass

To do this I've added a simple `domain` class (actually a record).
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

Well, try the tests again and two still fail! This is because the validation using `@Size` causes a `javax.validation.ConstraintViolationException`.
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

### The first story is complete on to the next.
This now meets the needs for the first paragraph.
Now we can work on the next story, that story is the business logic.

Just to recap the business logic is:
"A User Identifier is deemed acceptable if it does not have the letter `X` in it (arbitrary) and must not
contain any punctuation characters."



