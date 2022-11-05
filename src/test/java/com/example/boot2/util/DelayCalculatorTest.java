package com.example.boot2.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.security.InvalidParameterException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class DelayCalculatorTest {

  private final DelayCalculator underTest = new DelayCalculator(10000000);

  @ParameterizedTest
  @CsvSource({"500,9,999500", "5000000,5,0", "10000000,0,0", "20000000,0,0"})
  void testDelayCalculations(long duration, long expectedMilliSeconds, int expectedNanoSeconds) {
    var result = underTest.apply(duration);
    assertEquals(expectedMilliSeconds, result.delayMilliSeconds());
    assertEquals(expectedNanoSeconds, result.delayNanoSeconds());
  }

  @Test
  void testInvalidDelayCalculator() {
    var exception = assertThrows(InvalidParameterException.class,
        () -> new DelayCalculator(0));
    assertEquals("max time in nano seconds must be greater than 1", exception.getMessage());
  }
}
