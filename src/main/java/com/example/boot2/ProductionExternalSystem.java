package com.example.boot2;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * Example of a production external system.
 */
@Service
@ConditionalOnProperty(name = "external.system", havingValue = "prd")
public class ProductionExternalSystem implements ExternalSystem {
  @Override
  public String getExternalSystemName() {
    return "Production";
  }
}
