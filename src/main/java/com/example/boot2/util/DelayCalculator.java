package com.example.boot2.util;

import java.security.InvalidParameterException;
import java.util.function.LongFunction;

/**
 * Given a configured maximum time in nanoseconds, this calculator accepts a duration
 * and returns the delay in milliseconds/nanoseconds to sum to the maxTime.
 * If the duration is greater than maxTimeInNanoSeconds then zero will be returned for
 * th milliseconds and nanoseconds.
 */
public class DelayCalculator implements LongFunction<DelayCalculator.DelayPeriod> {

  private static final long NANOSECONDS_IN_MILLISECOND = 1000000L;

  private final long maxTimeInNanoSeconds;

  /**
   * Create a delay calculator for maxTime.
   */
  public DelayCalculator(final long maxTimeInNanoSeconds) {
    if (maxTimeInNanoSeconds < 1) {
      throw new InvalidParameterException("max time in nano seconds must be greater than 1");
    }
    this.maxTimeInNanoSeconds = maxTimeInNanoSeconds;
  }

  @Override
  public DelayPeriod apply(long duration) {

    var remainingNanos = (int) (maxTimeInNanoSeconds - duration);

    var delayMilliSeconds = (maxTimeInNanoSeconds - duration) / NANOSECONDS_IN_MILLISECOND;
    var delayNanoSeconds =
        (int) (remainingNanos - (delayMilliSeconds * NANOSECONDS_IN_MILLISECOND));

    //May have taken longer than maxTime.
    return remainingNanos > 0 ? new DelayPeriod(delayMilliSeconds, delayNanoSeconds) :
        new DelayPeriod(0, 0);
  }

  /**
   * The required delay period to make the total time when added to duration to be equal to
   * or greater than maxTime as configured.
   */
  public record DelayPeriod(long delayMilliSeconds, int delayNanoSeconds) {

  }
}


