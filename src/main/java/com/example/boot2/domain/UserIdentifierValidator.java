package com.example.boot2.domain;

import java.util.function.Supplier;

/**
 * Models the validator for checking user identifiers.
 */
public interface UserIdentifierValidator {

  Supplier<Status> validate(final String userIdentifier);
}
