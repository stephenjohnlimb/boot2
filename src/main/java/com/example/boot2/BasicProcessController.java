package com.example.boot2;

import com.example.boot2.domain.Status;
import com.example.boot2.domain.UserIdentifierValidator;
import java.util.function.BiFunction;
import java.util.function.Supplier;
import javax.validation.constraints.Size;
import org.springframework.http.HttpStatus;
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

  private final UserIdentifierValidator userIdentifierValidator;

  private final BiFunction<HttpStatus, Supplier<Status>, ResponseEntity<Status>> response =
      (statusCode, supplier) -> ResponseEntity.status(statusCode).body(supplier.get());

  public BasicProcessController(UserIdentifierValidator userIdentifierValidator) {
    this.userIdentifierValidator = userIdentifierValidator;
  }

  @GetMapping("/status/{userIdentifier}")
  public ResponseEntity<Status> checkInputValueStatus(
      @PathVariable("userIdentifier") @Size(min = 2, max = 30) String userIdentifier) {
    return response.apply(HttpStatus.OK, userIdentifierValidator.validate(userIdentifier));
  }
}
