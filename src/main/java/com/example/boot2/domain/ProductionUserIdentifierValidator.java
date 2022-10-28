package com.example.boot2.domain;

import java.util.Optional;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * The production validator.
 */
@Service
@ConditionalOnProperty(name = "run.system", havingValue = "prd")
public class ProductionUserIdentifierValidator implements UserIdentifierValidator {

  @Override
  public Supplier<Status> validate(final String userIdentifier) {
    var valid = isValid(userIdentifier);
    if (valid) {
      return () -> new Status(true, Optional.empty());
    }
    return () -> new Status(false, Optional.of("Fails Business Logic Check"));
  }

  private boolean isValid(String userIdentifier) {
    if (userIdentifier == null || userIdentifier.isBlank()) {
      return false;
    }
    if (userIdentifier.contains("X")) {
      return false;
    }
    if (Pattern.matches("(.*)[\\p{Punct}](.*)", userIdentifier)) {
      return false;
    }
    return true;
  }
}
