package com.example.boot2.domain;

import java.util.Optional;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Models the validator for checking user identifiers.
 */
public final class UserIdentifierValidator {
  private final Predicate<String> acceptableRule;

  private final Supplier<Status> valid =
      () -> new Status(true, Optional.empty());

  private final Supplier<Status> invalid =
      () -> new Status(false, Optional.of("Fails Business Logic Check"));

  public UserIdentifierValidator(Predicate<String> acceptableRule) {
    this.acceptableRule = acceptableRule;
  }

  /**
   * Validate the userIdentifier supplied using the configured rules.
   */
  public Supplier<Status> validate(final String userIdentifier) {

    return Optional.ofNullable(userIdentifier)
        .stream()
        .filter(acceptableRule)
        .findAny()
        .map(id -> valid)
        .orElse(invalid);
  }
}
