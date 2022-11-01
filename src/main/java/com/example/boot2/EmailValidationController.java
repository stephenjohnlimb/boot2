package com.example.boot2;

import com.example.boot2.domain.EmailValidator;
import com.example.boot2.domain.Status;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import java.util.function.BiFunction;
import java.util.function.Supplier;
import javax.validation.constraints.NotBlank;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * A controller that accepts requests to validate email addresses.
 * Responds with some JSON status data and a standard HTTP response.
 */
@RestController
@ResponseBody
@Validated
public class EmailValidationController {

  private final Logger logger = LoggerFactory.getLogger(EmailValidationController.class);

  private final EmailValidator emailValidator;

  private final BiFunction<HttpStatus, Supplier<Status>, ResponseEntity<Status>> response =
      (statusCode, supplier) -> ResponseEntity.status(statusCode).body(supplier.get());

  public EmailValidationController(EmailValidator emailValidator) {
    this.emailValidator = emailValidator;
  }

  /**
   * Email address structure validity checks.
   */
  @Operation(summary = "Check the validity of the 'email address' supplied")
  @GetMapping("/email/{emailAddress}")
  public ResponseEntity<Status> checkEmailAddress(
      @Parameter(description = "The 'email address' to be checked")
      @PathVariable("emailAddress") @NotBlank String emailAddress) {

    logger.info("Checking email validity of {}", emailAddress);
    return response.apply(HttpStatus.OK, emailValidator.apply(emailAddress));
  }
}
