package com.example.boot2.domain;

import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Ensures that the function called always takes 5 milliseconds.
 */
public class Delay
    implements BiFunction<String, Function<String, Supplier<Status>>, Supplier<Status>> {

  private static long maxTime = 5000000L;

  @Override
  public Supplier<Status> apply(String value,
                                Function<String, Supplier<Status>> function) {

    long startTime = System.nanoTime();
    var rtn = function.apply(value);
    long endTime = System.nanoTime();

    //We're trying to get every call to be N milliseconds.
    try {
      var totalNanos = maxTime - (int) (endTime - startTime);
      var remainingNanos = (int) (maxTime
          - (totalNanos % maxTime));
      var remainingMilliSeconds = (maxTime - remainingNanos) / 1000000;
      if (remainingNanos > 0 && remainingNanos < 1000000) {
        Thread.sleep(remainingMilliSeconds, remainingNanos);
      }
    } catch (InterruptedException interruptedException) {
      //Ignore
      Thread.currentThread().interrupt();
    }

    return rtn;
  }
}
