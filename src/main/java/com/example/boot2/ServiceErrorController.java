package com.example.boot2;

import javax.servlet.RequestDispatcher;
import javax.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Designed to provide a little more information to the caller in the event of an error.
 */
@Controller
public class ServiceErrorController implements ErrorController {

  private final Logger logger = LoggerFactory.getLogger(ServiceErrorController.class);

  /**
   * Handles any errors and maps through to an error page.
   */
  @RequestMapping("/error")
  public String handleError(HttpServletRequest request) {
    Object status = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
    logger.error("WebService Error {}", status);
    return "error";
  }
}
