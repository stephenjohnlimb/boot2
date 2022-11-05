package com.example.boot2.domain;

import java.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;

/**
 * User Identifier Validator that delegates to a Value Validator.
 */
public class UserIdentifierValidator implements Function<String, Status> {

  private final Logger logger = LoggerFactory.getLogger(UserIdentifierValidator.class);

  private final ValueValidator delegateValidator;

  public UserIdentifierValidator(final ValueValidator validator) {
    this.delegateValidator = validator;
  }

  @Override
  @Cacheable(value = "status", key = "#userIdentifier")
  public Status apply(String userIdentifier) {
    logger.info("Checking status of {}", userIdentifier);
    return delegateValidator.validate(userIdentifier).get();
  }
}
