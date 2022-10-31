## Continuing with Agile Development

If you've read [the previous page on the development so far](AgileDevelopment.md) you'll recall the
requirement is as follows:

>We need an application that can accept a user identifier and tell us if that user identifier is acceptable.
If it is not acceptable we also want some reasoning as to why. We will only submit user identifier values of
length greater than 1 and less than 31 characters.

>A User Identifier is deemed acceptable if it does not have the letter `X` in it (arbitrary) and must not
contain any punctuation characters, and is not blank (i.e just spaces).

>The application must be deployable into a Kubernetes cluster.

### The new requirement

>Extend the validation microservice to be able to accept requests to validate email addresses and respond
> with the email is acceptable or not.

>The email address is deemed valid if it complies with [this definition](https://help.xmatters.com/ondemand/trial/valid_email_format.htm).

Note that the definition above is just one I picked, real email addresses can vary from this (but this will do for this example).

So this should be too hard to do as a couple of stories. Then may be one final story to hook it all up.

#### `Story 1`

Well that's just the first paragraph, we will define a new controller and service a URL on it.

First the tests on the non-existent controller
```
...
@SpringBootTest
@AutoConfigureMockMvc
class EmailControllerTests {

  @Test
  void testGetStatusOfUser(@Autowired MockMvc mvc) throws Exception {
    mvc.perform(get("/email/StephenJohnLimb@mail.com")).andExpect(status().isOk());
  }

  @Test
  void testGetStatusNotFound(@Autowired MockMvc mvc) throws Exception {
    mvc.perform(get("/email")).andExpect(status().is(404));
  }

  @Test
  void testGetStatusNotFoundEmpty(@Autowired MockMvc mvc) throws Exception {
    mvc.perform(get("/email/")).andExpect(status().is(404));
  }

  @Test
  void testGetStatusNotFoundBlank(@Autowired MockMvc mvc) throws Exception {
    mvc.perform(get("/email/ ")).andExpect(status().is(412));
  }
```

They all fail! So let's add a new controller.

Here's that basic controller:
```
@RestController
@ResponseBody
@Validated
public class EmailValidationController {

  private final Supplier<Status> acceptable =
      () -> new Status(true, Optional.empty());

  private final BiFunction<HttpStatus, Supplier<Status>, ResponseEntity<Status>> response =
      (statusCode, supplier) -> ResponseEntity.status(statusCode).body(supplier.get());

  @GetMapping("/email/{emailAddress}")
  public ResponseEntity<Status> checkEmailAddress(
      @PathVariable("emailAddress") @NotBlank String emailAddress) {
    return response.apply(HttpStatus.OK, acceptable);
  }
}
```

So that's it. We're done. We've checked the data can come in and be processed.

Now in parallel other members of the team have been working on `Story 2` below.
#### `Story 2`

That's the second paragraph, just the business logic.

Now this is a bit more interesting, the logic of the processing seems almost the same as the
userIdentifierValidator just the `Predicates` and `Suppliers` (message text) is different.

So let's refactor! First pull the `valid` and `invalid` `Suppliers` and inject them via the Configuration.
But also we will rename this class, as it is now just a general sort of validator and has nothing really to
do with "User Identifiers" at all.
```
public final class ValueValidator {

  private final Predicate<String> acceptableRule;
  private final Supplier<Status> valid;
  private final Supplier<Status> invalid;

  public ValueValidator(Predicate<String> acceptableRule,
                        Supplier<Status> validStatus, Supplier<Status> invalidStatus) {
    this.acceptableRule = acceptableRule;
    this.valid = validStatus;
    this.invalid = invalidStatus;
  }

  /**
   * Validate the value supplied using the configured rules.
   */
  public Supplier<Status> validate(final String value) {

    return Optional.ofNullable(value)
        .stream()
        .filter(acceptableRule)
        .findAny()
        .map(id -> valid)
        .orElse(invalid);
  }
}
```

We need to alter the `ValidatorConfiguration` (I've added in a dummy emailValidator) as below:
```
@Configuration
public class ValidatorConfiguration {

  private final Supplier<Status> valid =
      () -> new Status(true, Optional.empty());

  private final Supplier<Status> userIdentifierInvalid =
      () -> new Status(false, Optional.of("Fails Business Logic Check"));

  private final Supplier<Status> emailAddressInvalid =
      () -> new Status(false, Optional.of("Fails Email Validation Check"));

  @Bean("userIdentifierValidator")
  @ConditionalOnProperty(name = "run.system", havingValue = "stub")
  ValueValidator stubUserIdentifierValidator() {
    return new ValueValidator(userIdentifier -> true, valid, userIdentifierInvalid);
  }

  @Bean("userIdentifierValidator")
  @ConditionalOnProperty(name = "run.system", havingValue = "prd")
  ValueValidator productionUserIdentifierValidator() {

    final Predicate<String> hasValue =
        userIdentifier -> userIdentifier != null && !userIdentifier.isBlank();

    final Predicate<String> doesNotContainX =
        userIdentifier -> !userIdentifier.contains("X");

    final Predicate<String> doesNotContainPunctuation =
        userIdentifier -> !Pattern.matches("(.*)[\\p{Punct}](.*)", userIdentifier);

    final Predicate<String> rules =
        hasValue.and(doesNotContainX).and(doesNotContainPunctuation);

    return new ValueValidator(rules, valid, userIdentifierInvalid);
  }

  @Bean("emailValidator")
  @ConditionalOnProperty(name = "run.system", havingValue = "stub")
  ValueValidator stubEmailAddressValidator() {
    return new ValueValidator(userIdentifier -> true, valid, emailAddressInvalid);
  }

  @Bean("emailValidator")
  @ConditionalOnProperty(name = "run.system", havingValue = "prd")
  ValueValidator productionEmailAddressValidator() {
    return new ValueValidator(userIdentifier -> true, valid, emailAddressInvalid);
  }
}
```

Now we can re-use the `ValueValidator` but use rules around email validation checks.
But notice I now need to use 'names' like `emailValidator` and `userIdentifierValidator`.

This is because we'd have two beans of the same type - even though configured differently.

So I now have to revisit `BasicProcessController` (not nice).  
```
...
 public BasicProcessController(@Qualifier("userIdentifierValidator") ValueValidator userIdentifierValidator) {
    this.userIdentifierValidator = userIdentifierValidator;
  }
...
```

We can now create our email validation tests:
```
class EmailValidatorTest {

  private final ValueValidator underTest = new ValidatorConfiguration().productionEmailAddressValidator();

  private final Consumer<Supplier<Status>> assertFailsBusinessLogic = supplier -> {
    var result = supplier.get();
    assertFalse(result.acceptable());
    assertTrue(result.reasonUnacceptable().isPresent());
    assertEquals("Fails Email Validation Check", result.reasonUnacceptable().get());
  };

  @ParameterizedTest
  @CsvSource({"abc-d@mail.com", "abc.def@mail.com", "abc@mail.com", "abc_def@mail.com"})
  void testValidEmailPrefix(String toBeValidated) {
    var result = underTest.validate(toBeValidated).get();
    assertTrue(result.acceptable());
    assertTrue(result.reasonUnacceptable().isEmpty());
  }

  @ParameterizedTest
  @CsvSource({"abc.def@mail.cc", "abc.def@mail-archive.com", "abc.def@mail.org", "abc.def@mail.com"})
  void testValidEmailDomain(String toBeValidated) {
    var result = underTest.validate(toBeValidated).get();
    assertTrue(result.acceptable());
    assertTrue(result.reasonUnacceptable().isEmpty());
  }

  @ParameterizedTest
  @CsvSource({"abc-@mail.com", "abc..def@mail.com", ".abc@mail.com", "abc#def@mail.com", "@mail.com"})
  void testInvalidEmailPrefix(String toBeValidated) {
    assertFailsBusinessLogic.accept(underTest.validate(toBeValidated));
  }

  @ParameterizedTest
  @CsvSource({"abc.def@mail.c", "abc.def@mail#archive.com", "abc.def@mail", "abc.def@mail..com", "abc.def@"})
  void testInvalidEmailDomain(String toBeValidated) {
    assertFailsBusinessLogic.accept(underTest.validate(toBeValidated));
  }

  @ParameterizedTest
  @NullSource  // pass a null value
  @ValueSource(strings = {"", " ", "    ", "@", " @ ", "   @   "})
  void testNullAndBlankContent(String toBeValidated) {
    assertFailsBusinessLogic.accept(underTest.validate(toBeValidated));
  }
}
```

So quite a few of these tests fail - which in not surprising because we've not implemented anything yet.

Here is a basic simple implementation that will enable all the tests to pass.
```
public class EmailValidation implements Predicate<String> {

  private static final String EMAIL_REGEX =
      "^[a-zA-Z0-9]+[._-]?[a-zA-Z0-9]+@(([a-zA-Z\\-0-9]*+)\\.[a-zA-Z]{2,})$";

  private static final Pattern pattern = Pattern.compile(EMAIL_REGEX);

  @Override
  public boolean test(final String email) {
    return pattern.matcher(email).matches();
  }
}
```

I'll just update `ValidatorConfiguration` with this implementation:
```
...
@Bean("emailValidator")
  @ConditionalOnProperty(name = "run.system", havingValue = "prd")
  ValueValidator productionEmailAddressValidator() {
    return new ValueValidator(new EmailValidation(), valid, emailAddressInvalid);
  }
...
```

So almost there, just need to implement `story 3`

#### `Story 3`

Ensure that the business logic is plugged into the controller, as below:
```
@RestController
@ResponseBody
@Validated
public class EmailValidationController {

  private final ValueValidator emailValidator;

  private final BiFunction<HttpStatus, Supplier<Status>, ResponseEntity<Status>> response =
      (statusCode, supplier) -> ResponseEntity.status(statusCode).body(supplier.get());

  public EmailValidationController(
    @Qualifier("emailValidator") ValueValidator emailValidator) {
      this.emailValidator = emailValidator;
  }

  @GetMapping("/email/{emailAddress}")
  public ResponseEntity<Status> checkEmailAddress(
      @PathVariable("emailAddress") @NotBlank String emailAddress) {
    return response.apply(HttpStatus.OK, emailValidator.validate(emailAddress));
  }
}
```

## Summary (sort of)

Give it a quick test - end-2-end:
```
curl http://localhost:8080/email/StephenJohnLimb@gmail.com
# The output
{"acceptable":true,"reasonUnacceptable":null}

curl http://localhost:8080/email/StephenJohnLimb@gmail.c
# The output
{"acceptable":false,"reasonUnacceptable":"Fails Email Validation Check"}

# Check 'status' with user identification still works.
curl http://localhost:8080/status/StephenJohnLimb

# The output
{"acceptable":true,"reasonUnacceptable":null} 
```

OK now bump the application version in build.gradle and Chart.yaml; then we can redeploy via helm
and check it actually runs in the K8S cluster.

```
kubectl get services

#The output
NAME                            TYPE           CLUSTER-IP       EXTERNAL-IP     PORT(S)                      AGE
boot2                           LoadBalancer   10.152.183.198   192.168.64.95   80:32625/TCP                 65s

curl http://192.168.64.95/email/StephenJohnLimb@gmail.com
# The output
{"acceptable":true,"reasonUnacceptable":null}

curl http://192.168.64.95/email/StephenJohnLimb@gmail.c
# The output
{"acceptable":false,"reasonUnacceptable":"Fails Email Validation Check"}

# Check 'status' with user identification still works.
curl http://192.168.64.95/status/StephenJohnLimb

# The output
{"acceptable":true,"reasonUnacceptable":null} 
```

So we're all good.

Now there's a couple of things I don't like - "Qualified beans by name".
The alternatives would be to:
- Subclass ValueValidator with a class that works for emails and another for user identifiers
- Or, a class (or Function) one for email and one for user identifiers that delegates to the ValueValidator

This would remove the "Qualification" Springy stuff - which I find a bit busy on my eye.

At I have a bit of time, let's refactor again. All the tests are passing and the whole thing runs.
But I would like to see if I can avoid the bean qualifiers, a bit anal I know. But let's see what's involved.

### Refactoring away the Bean Qualifiers

I've added the following:
```
public final class EmailValidator implements Function<String, Supplier<Status>> {

  private final ValueValidator delegateValidator;

  public EmailValidator(final ValueValidator validator) {
    this.delegateValidator = validator;
  }

  @Override
  public Supplier<Status> apply(String emailValue) {
    return delegateValidator.validate(emailValue);
  }
}
```

And
```
public final class UserIdentifierValidator implements Function<String, Supplier<Status>> {

  private final ValueValidator delegateValidator;

  public UserIdentifierValidator(final ValueValidator validator) {
    this.delegateValidator = validator;
  }

  @Override
  public Supplier<Status> apply(String userIdentifierValue) {
    return delegateValidator.validate(userIdentifierValue);
  }
}
```

Then altered the Configuration as follows:
```
@Configuration
public class ValidatorConfiguration {

  private final Supplier<Status> valid =
      () -> new Status(true, Optional.empty());

  private final Supplier<Status> userIdentifierInvalid =
      () -> new Status(false, Optional.of("Fails Business Logic Check"));

  private final Supplier<Status> emailAddressInvalid =
      () -> new Status(false, Optional.of("Fails Email Validation Check"));

  @Bean
  @ConditionalOnProperty(name = "run.system", havingValue = "stub")
  public UserIdentifierValidator stubUserIdentifierValidator() {
    return new UserIdentifierValidator(new ValueValidator(userIdentifier -> true, valid, userIdentifierInvalid));
  }

  @Bean
  @ConditionalOnProperty(name = "run.system", havingValue = "prd")
  public UserIdentifierValidator productionUserIdentifierValidator() {

    final Predicate<String> hasValue =
        userIdentifier -> userIdentifier != null && !userIdentifier.isBlank();

    final Predicate<String> doesNotContainX =
        userIdentifier -> !userIdentifier.contains("X");

    final Predicate<String> doesNotContainPunctuation =
        userIdentifier -> !Pattern.matches("(.*)[\\p{Punct}](.*)", userIdentifier);

    final Predicate<String> rules =
        hasValue.and(doesNotContainX).and(doesNotContainPunctuation);

    return new UserIdentifierValidator(new ValueValidator(rules, valid, userIdentifierInvalid));
  }

  @Bean
  @ConditionalOnProperty(name = "run.system", havingValue = "stub")
  public EmailValidator stubEmailAddressValidator() {
    return new EmailValidator(new ValueValidator(userIdentifier -> true, valid, emailAddressInvalid));
  }

  @Bean
  @ConditionalOnProperty(name = "run.system", havingValue = "prd")
  public EmailValidator productionEmailAddressValidator() {
    return new EmailValidator(new ValueValidator(new EmailValidation(), valid, emailAddressInvalid));
  }
}
```

That now means I can alter the controllers as follows:
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
    return response.apply(HttpStatus.OK, userIdentifierValidator.apply(userIdentifier));
  }
}
```

And
```
@RestController
@ResponseBody
@Validated
public class EmailValidationController {

  private final EmailValidator emailValidator;

  private final BiFunction<HttpStatus, Supplier<Status>, ResponseEntity<Status>> response =
      (statusCode, supplier) -> ResponseEntity.status(statusCode).body(supplier.get());

  public EmailValidationController(EmailValidator emailValidator) {
      this.emailValidator = emailValidator;
  }

  @GetMapping("/email/{emailAddress}")
  public ResponseEntity<Status> checkEmailAddress(
      @PathVariable("emailAddress") @NotBlank String emailAddress) {
    return response.apply(HttpStatus.OK, emailValidator.apply(emailAddress));
  }
}
```

So there is more code and some of it is a bit duplicated, it's more 'Java' and less 'Spring'. Now if
Java had Generics that did not do type erasure we could have parameterised a generic version of
`UserIdentifierValidator` and `EmailValidator` without all the clumsy `@Qualifier` stuff.

But it is clearer on my eye. So if you've a very 'Spring person' you'd be happy with all these
bean names and qualifiers, but I'm more of a just 'Java person'. In fact if you look only
`ValidatorConfiguration` in the domain package has any sort of 'Spring' stuff - I quite like that.

## The Actual Summary
Well you may or may not agree with my approach here, but I think you'll agree, less 'Spring' is better,
more composition is better and enables you to move stuff around more.

So all of this (including writing this waffle) took a couple of hours. That's pretty Agile.

The smaller, more single function, isolated and composed your classes and functions are the more
you can just move them around. Also, if you focus on test driven development - you can focus on the
isolation and get teams to do parallel development and then plug it all together as it's done.