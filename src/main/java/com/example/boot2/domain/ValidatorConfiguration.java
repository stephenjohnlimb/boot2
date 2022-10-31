package com.example.boot2.domain;

import java.util.Optional;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Defines the configuration for the UserIdentifierValidation bean.
 */
@Configuration
public class ValidatorConfiguration {

  private final Supplier<Status> valid = () -> new Status(true, Optional.empty());

  private final Supplier<Status> userIdentifierInvalid =
      () -> new Status(false, Optional.of("Fails Business Logic Check"));

  private final Supplier<Status> emailAddressInvalid =
      () -> new Status(false, Optional.of("Fails Email Validation Check"));

  @Bean
  @ConditionalOnProperty(name = "run.system", havingValue = "stub")
  UserIdentifierValidator stubUserIdentifierValidator() {
    return new UserIdentifierValidator(
        new ValueValidator(userIdentifier -> true, valid, userIdentifierInvalid));
  }

  @Bean
  @ConditionalOnProperty(name = "run.system", havingValue = "prd")
  UserIdentifierValidator productionUserIdentifierValidator() {

    final Predicate<String> hasValue =
        userIdentifier -> userIdentifier != null && !userIdentifier.isBlank();

    final Predicate<String> doesNotContainX = userIdentifier -> !userIdentifier.contains("X");

    final Predicate<String> doesNotContainPunctuation =
        userIdentifier -> !Pattern.matches("(.*)[\\p{Punct}](.*)", userIdentifier);

    final Predicate<String> rules = hasValue.and(doesNotContainX).and(doesNotContainPunctuation);

    return new UserIdentifierValidator(new ValueValidator(rules, valid, userIdentifierInvalid));
  }

  @Bean
  @ConditionalOnProperty(name = "run.system", havingValue = "stub")
  EmailValidator stubEmailAddressValidator() {
    return new EmailValidator(
        new ValueValidator(userIdentifier -> true, valid, emailAddressInvalid));
  }

  @Bean
  @ConditionalOnProperty(name = "run.system", havingValue = "prd")
  EmailValidator productionEmailAddressValidator() {
    return new EmailValidator(
        new ValueValidator(new EmailValidation(), valid, emailAddressInvalid));
  }
}

