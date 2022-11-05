package com.example.boot2.util;

import java.util.function.Function;

/**
 * Times a function call and gives both the duration in nanoseconds and the functions return value.
 */
public class FunctionTimer<T, R> implements Function<T, FunctionTimer.TimerResult<R>> {

  private final Function<T, R> toBeTimed;

  public FunctionTimer(Function<T, R> toBeTimed) {
    this.toBeTimed = toBeTimed;
  }

  @Override
  public TimerResult<R> apply(T value) {
    long startTime = System.nanoTime();
    var rtn = toBeTimed.apply(value);
    long endTime = System.nanoTime();
    return new TimerResult<>(endTime - startTime, rtn);
  }

  /**
   * Record for the duration of the time the function took and the return value from the function.
   */
  public record TimerResult<R>(long functionDurationNanoSeconds, R functionResult) {
  }
}
