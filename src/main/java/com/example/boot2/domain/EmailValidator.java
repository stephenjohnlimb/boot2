package com.example.boot2.domain;

import java.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;

/**
 * Email Validator that delegates to a Value Validator.
 * Just provides strong typing and wrapping as a function.
 */
public class EmailValidator implements Function<String, Status> {
  private final Logger logger = LoggerFactory.getLogger(EmailValidator.class);

  private final ValueValidator delegateValidator;

  public EmailValidator(final ValueValidator validator) {
    this.delegateValidator = validator;
  }

  @Override
  @Cacheable(value = "email", key = "#emailAddress")
  public Status apply(String emailAddress) {
    logger.info("Checking email validity of {}", emailAddress);
    return delegateValidator.validate(emailAddress).get();
  }
}
