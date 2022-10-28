package com.example.boot2.domain;

import java.util.Optional;
import java.util.function.Supplier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "run.system", havingValue = "stub")
public class TestUserIdentifierValidator implements UserIdentifierValidator {

  @Override
  public Supplier<Status> validate(final String userIdentifier) {
    return () -> new Status(true, Optional.empty());
  }
}
