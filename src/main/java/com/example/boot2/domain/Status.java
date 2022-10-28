package com.example.boot2.domain;

import java.util.Optional;

/**
 * The domain model of some for of check response status.
 *
 * @param acceptable if true then status was ok, false then not ok.
 * @param reasonUnacceptable if not ok then the optional will have some text reason.
 */
public record Status(boolean acceptable, Optional<String> reasonUnacceptable) {
}
