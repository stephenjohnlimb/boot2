package com.example.boot2;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Example of a test external system.
 */
@Component
@ConditionalOnProperty(name = "external.system", havingValue = "stub")
public class TestExternalSystem implements ExternalSystem {
  @Override
  public String getExternalSystemName() {
    return "Test";
  }
}
