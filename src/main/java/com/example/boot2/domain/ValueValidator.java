package com.example.boot2.domain;

import java.util.Optional;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Models the validator for checking incoming values.
 * Can be configured with a different Predicates and suppliers of responses.
 */
public final class ValueValidator {

  private final Predicate<String> acceptableRule;
  private final Supplier<Status> valid;
  private final Supplier<Status> invalid;

  /**
   * Validator using predicates and suppliers of valid and invalid status.
   */
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
