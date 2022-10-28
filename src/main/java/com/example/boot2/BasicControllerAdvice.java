package com.example.boot2;

import com.example.boot2.domain.Status;
import java.util.Optional;
import javax.servlet.http.HttpServletRequest;
import javax.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * This is needed because I'm using 'import javax.validation.constraints.Size' on
 * the incoming params.
 * There a nice, but just throw exceptions. So we need to use this 'aspect programming model'.
 * This defines all the 'advice' around the method calls. So now we can define this bean and deal
 * with the exception (ConstraintViolationException) and alter the response code from being a 500
 * to something more useful like a 400 - but include the error message as to why we have rejected
 * the incoming request.
 */
@ControllerAdvice(basePackageClasses = BasicProcessController.class)
public class BasicControllerAdvice extends ResponseEntityExceptionHandler {

  /**
   * Deals with an exception when any of the methods on BasicProcessController are called.
   */
  @ResponseBody
  @ExceptionHandler(ConstraintViolationException.class)
  public ResponseEntity<Status> handlerControllerException(HttpServletRequest request,
                                                           Throwable th) {
    return ResponseEntity
        .status(HttpStatus.PRECONDITION_FAILED)
        .body(new Status(false, Optional.of(th.getMessage())));
  }
}
