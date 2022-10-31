package com.example.boot2.domain;

import java.util.function.Function;
import java.util.function.Supplier;

/**
 * User Identifier Validator that delegates to a Value Validator.
 */
public final class UserIdentifierValidator implements Function<String, Supplier<Status>> {

  private final ValueValidator delegateValidator;

  public UserIdentifierValidator(final ValueValidator validator) {
    this.delegateValidator = validator;
  }

  @Override
  public Supplier<Status> apply(String userIdentifierValue) {
    return delegateValidator.validate(userIdentifierValue);
  }
}
