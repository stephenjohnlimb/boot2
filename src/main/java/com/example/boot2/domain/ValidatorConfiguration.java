package com.example.boot2.domain;

import java.util.function.Predicate;
import java.util.regex.Pattern;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Defines the configuration for the UserIdentifierValidation bean.
 */
@Configuration
public class ValidatorConfiguration {

  @Bean
  @ConditionalOnProperty(name = "run.system", havingValue = "stub")
  UserIdentifierValidator stubValidator() {
    return new UserIdentifierValidator(userIdentifier -> true);
  }

  @Bean
  @ConditionalOnProperty(name = "run.system", havingValue = "prd")
  UserIdentifierValidator productionValidator() {

    final Predicate<String> hasValue =
        userIdentifier -> userIdentifier != null && !userIdentifier.isBlank();

    final Predicate<String> doesNotContainX =
        userIdentifier -> !userIdentifier.contains("X");

    final Predicate<String> doesNotContainPunctuation =
        userIdentifier -> !Pattern.matches("(.*)[\\p{Punct}](.*)", userIdentifier);

    final Predicate<String> rules =
        hasValue.and(doesNotContainX).and(doesNotContainPunctuation);

    return new UserIdentifierValidator(rules);
  }
}

