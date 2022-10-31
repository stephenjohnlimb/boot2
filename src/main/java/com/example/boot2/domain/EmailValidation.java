package com.example.boot2.domain;

import java.util.function.Predicate;
import java.util.regex.Pattern;

/**
 * Predicate for checking email string are reasonably valid.
 */
public class EmailValidation implements Predicate<String> {

  private static final String EMAIL_REGEX =
      "^[a-zA-Z0-9]+[._-]?[a-zA-Z0-9]+@(([a-zA-Z\\-0-9]*+)\\.[a-zA-Z]{2,})$";

  private static final Pattern pattern = Pattern.compile(EMAIL_REGEX);

  @Override
  public boolean test(final String email) {
    return pattern.matcher(email).matches();
  }
}
