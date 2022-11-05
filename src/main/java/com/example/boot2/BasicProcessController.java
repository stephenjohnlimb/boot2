package com.example.boot2;

import com.example.boot2.domain.Status;
import com.example.boot2.domain.UserIdentifierValidator;
import com.example.boot2.util.Delay;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import javax.validation.constraints.Size;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * A controller that accepts requests, checks some fields and
 * response with some JSON status data and a standard HTTP response.
 */
@RestController
@ResponseBody
@Validated
public class BasicProcessController {

  private final RequestProcessor requestProcessor;

  public BasicProcessController(UserIdentifierValidator userIdentifierValidator) {
    requestProcessor =
        new RequestProcessor(new Delay<>(10000000, userIdentifierValidator));
  }

  /**
   * Status checks.
   */
  @Operation(summary = "Check the status of the 'user identifier' supplied")
  @GetMapping("/status/{userIdentifier}")
  public ResponseEntity<Status> checkInputValueStatus(
      @Parameter(description = "The 'user identifier' to be checked")
      @PathVariable("userIdentifier") @Size(min = 2, max = 30) String userIdentifier) {

    return requestProcessor.apply(userIdentifier);
  }
}
