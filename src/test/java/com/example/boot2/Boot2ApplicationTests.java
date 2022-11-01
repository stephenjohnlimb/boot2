package com.example.boot2;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;

/**
 * This is designed to only test the controller.
 * It is not designed to test all the business logic - that is done elsewhere.
 *
 * So really we're focussed on does the URL exist, are the parameters being sent in OK.
 */
@SpringBootTest
@AutoConfigureMockMvc
class Boot2ApplicationTests {

  @Autowired
  ApplicationContext applicationContext;

  @Test
  void contextLoads() {
    assertNotNull(applicationContext);
  }

  @Test
  void testGetStatusOfUser(@Autowired MockMvc mvc) throws Exception {
    mvc.perform(get("/status/SteveLimb")).andExpect(status().isOk());
  }

  @Test
  void testGetStatusNotFound(@Autowired MockMvc mvc) throws Exception {
    mvc.perform(get("/status")).andExpect(status().is(404));
  }

  @Test
  void testGetStatusBadUserIdentifierLengthTooShort(@Autowired MockMvc mvc) throws Exception {
    mvc.perform(get("/status/s")).andExpect(status().is(412));
  }

  @Test
  void testGetStatusBadUserIdentifierLengthTooLong(@Autowired MockMvc mvc) throws Exception {
    mvc.perform(get("/status/s123456789012345678901234567890")).andExpect(status().is(412));
  }
}
