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
class UserIdentifierValidatorTest {

  private final UserIdentifierValidator underTest =
      new ValidatorConfiguration().productionUserIdentifierValidator();

  private final Consumer<Status> assertFailsBusinessLogic = result -> {
    assertFalse(result.acceptable());
    assertTrue(result.reasonUnacceptable().isPresent());
    assertEquals("Fails Business Logic Check", result.reasonUnacceptable().get());
  };

  @ParameterizedTest
  @CsvSource({"Steve", "StephenLimb", "Stephen John Limb"})
  void testAcceptableContent(String toBeValidated) {
    var result = underTest.apply(toBeValidated);
    assertTrue(result.acceptable());
    assertTrue(result.reasonUnacceptable().isEmpty());
  }

  @ParameterizedTest
  @CsvSource({"SteveX", "Stephen X Limb"})
  void testXContent(String toBeValidated) {
    assertFailsBusinessLogic.accept(underTest.apply(toBeValidated));
  }

  @ParameterizedTest
  @CsvSource({"StephenLimb!", "@StephenLimb", "@", "!", "{"})
  void testPunctuatedContent(String toBeValidated) {
    assertFailsBusinessLogic.accept(underTest.apply(toBeValidated));
  }

  @ParameterizedTest
  @NullSource  // pass a null value
  @ValueSource(strings = {"", " ", "    "})
  void testNullAndBlankContent(String toBeValidated) {
    assertFailsBusinessLogic.accept(underTest.apply(toBeValidated));
  }
}
