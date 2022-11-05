package com.example.boot2.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.function.Consumer;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

//Suppressed because sonarlint can't work out this consumer is doing the assertions.
@SuppressWarnings("java:S2699")
class EmailValidatorTest {

  private final EmailValidator underTest =
      new ValidatorConfiguration().productionEmailAddressValidator();

  private final Consumer<Status> assertFailsBusinessLogic = result -> {
    assertFalse(result.acceptable());
    assertTrue(result.reasonUnacceptable().isPresent());
    assertEquals("Fails Email Validation Check", result.reasonUnacceptable().get());
  };

  @ParameterizedTest
  @CsvSource({"abc-d@mail.com", "abc.def@mail.com", "abc@mail.com", "abc_def@mail.com"})
  void testValidEmailPrefix(String toBeValidated) {
    var result = underTest.apply(toBeValidated);
    assertTrue(result.acceptable());
    assertTrue(result.reasonUnacceptable().isEmpty());
  }

  @ParameterizedTest
  @CsvSource({"abc.def@mail.cc", "abc.def@mail-archive.com", "abc.def@mail.org",
      "abc.def@mail.com"})
  void testValidEmailDomain(String toBeValidated) {
    var result = underTest.apply(toBeValidated);
    assertTrue(result.acceptable());
    assertTrue(result.reasonUnacceptable().isEmpty());
  }

  @ParameterizedTest
  @CsvSource({"abc-@mail.com", "abc..def@mail.com", ".abc@mail.com", "abc#def@mail.com",
      "@mail.com"})
  void testInvalidEmailPrefix(String toBeValidated) {
    assertFailsBusinessLogic.accept(underTest.apply(toBeValidated));
  }

  @ParameterizedTest
  @CsvSource({"abc.def@mail.c", "abc.def@mail#archive.com", "abc.def@mail", "abc.def@mail..com",
      "abc.def@"})
  void testInvalidEmailDomain(String toBeValidated) {
    assertFailsBusinessLogic.accept(underTest.apply(toBeValidated));
  }

  @ParameterizedTest
  @NullSource  // pass a null value
  @ValueSource(strings = {"", " ", "    ", "@", " @ ", "   @   "})
  void testNullAndBlankContent(String toBeValidated) {
    assertFailsBusinessLogic.accept(underTest.apply(toBeValidated));
  }
}
