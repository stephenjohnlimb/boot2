package com.example.boot2.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.function.Consumer;
import java.util.function.Supplier;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

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
