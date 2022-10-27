package com.example.boot2;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Simple Spring Boot app, for experimenting.
 */
@SpringBootApplication
public class Boot2Application {

  private final Logger logger = LoggerFactory.getLogger(Boot2Application.class);
  private final ExternalSystem externalSystem;

  public Boot2Application(ExternalSystem externalSystem) {
    this.externalSystem = externalSystem;
    displayExternalSystemInUse();
  }

  private void displayExternalSystemInUse() {
    logger.info("Created with externalSystem {}", externalSystem.getExternalSystemName());
  }

  public static void main(String[] args) {
    SpringApplication.run(Boot2Application.class, args);
  }

}
