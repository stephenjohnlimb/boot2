package com.example.boot2;

import com.example.boot2.domain.Status;
import java.util.function.Function;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * Just processes the incoming value using a validator function.
 * Wraps the function call and maps to a ResponseEntity with Ok status.
 */
public class RequestProcessor implements Function<String, ResponseEntity<Status>> {

  private final Function<String, Status> wrapperFunction;

  public RequestProcessor(Function<String, Status> wrapperFunction) {
    this.wrapperFunction = wrapperFunction;
  }

  @Override
  public ResponseEntity<Status> apply(String value) {
    return ResponseEntity.status(HttpStatus.OK).body(wrapperFunction.apply(value));
  }
}
