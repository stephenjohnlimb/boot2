package com.example.boot2;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * Example of a test external system.
 */
@Service
@ConditionalOnProperty(name = "external.system", havingValue = "stub")
public class TestExternalSystem implements ExternalSystem {
  @Override
  public String getExternalSystemName() {
    return "Test";
  }
}
