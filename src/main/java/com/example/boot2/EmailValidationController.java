package com.example.boot2;

import com.example.boot2.domain.EmailValidator;
import com.example.boot2.domain.Status;
import com.example.boot2.util.Delay;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import javax.validation.constraints.NotBlank;
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

  private final RequestProcessor requestProcessor;

  public EmailValidationController(EmailValidator emailValidator) {
    requestProcessor = new RequestProcessor(new Delay<>(10000000, emailValidator));
  }

  /**
   * Email address structure validity checks.
   */
  @Operation(summary = "Check the validity of the 'email address' supplied")
  @GetMapping("/email/{emailAddress}")
  public ResponseEntity<Status> checkEmailAddress(
      @Parameter(description = "The 'email address' to be checked") @PathVariable("emailAddress")
      @NotBlank String emailAddress) {

    return requestProcessor.apply(emailAddress);
  }
}
