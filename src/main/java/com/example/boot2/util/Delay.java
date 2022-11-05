package com.example.boot2.util;

import java.util.function.Function;

/**
 * Ensures that the function called always takes 'delayPeriod'.
 */
public class Delay<T, R> implements Function<T, R> {

  private final DelayCalculator delayCalculator;

  private final FunctionTimer<T, R> functionTimer;

  public Delay(long delayPeriod, Function<T, R> functionToDelay) {
    this.delayCalculator = new DelayCalculator(delayPeriod);
    this.functionTimer = new FunctionTimer<>(functionToDelay);
  }

  @Override
  public R apply(T value) {

    FunctionTimer.TimerResult<R> result = functionTimer.apply(value);

    //We're trying to get every call to be N milliseconds.
    try {
      var delay = delayCalculator.apply(result.functionDurationNanoSeconds());

      //Sometimes it can take longer than the delayPeriod
      if (delay.delayMilliSeconds() >= 0 || delay.delayNanoSeconds() >= 0) {
        Thread.sleep(delay.delayMilliSeconds(), delay.delayNanoSeconds());
      }
    } catch (InterruptedException interruptedException) {
      //Ignore
      Thread.currentThread().interrupt();
    }

    return result.functionResult();
  }
}
