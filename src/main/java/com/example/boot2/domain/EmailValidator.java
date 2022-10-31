package com.example.boot2.domain;

import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Email Validator that delegates to a Value Validator.
 * Just provides strong typing and wrapping as a function.
 */
public final class EmailValidator implements Function<String, Supplier<Status>> {

  private final ValueValidator delegateValidator;

  public EmailValidator(final ValueValidator validator) {
    this.delegateValidator = validator;
  }

  @Override
  public Supplier<Status> apply(String emailValue) {
    return delegateValidator.validate(emailValue);
  }
}
