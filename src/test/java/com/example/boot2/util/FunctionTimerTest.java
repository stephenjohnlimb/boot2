package com.example.boot2.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.function.Function;
import org.junit.jupiter.api.Test;

class FunctionTimerTest {

  @Test
  //Could use Awaitility - but want to keep this simple.
  @SuppressWarnings("java:S2925")
  void testFunctionTimer() {

    Function<String, Integer> fixture = value -> {
      try {
        //force a delay, so we can check the timer worked.
        Thread.sleep(2);
      } catch (InterruptedException iex) {
        //ignore
        assertTrue(Thread.interrupted());
      }
      return value.length();
    };

    FunctionTimer<String, Integer> underTest = new FunctionTimer<>(fixture);

    var result = underTest.apply("AnyText");

    assertTrue(result.functionDurationNanoSeconds() >= 2000000);
    assertEquals(7, result.functionResult());
  }
}
