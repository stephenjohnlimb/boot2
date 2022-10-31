package com.example.boot2;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

/**
 * This is designed to only test the email validation controller.
 * So really we're focussed on does the URL exist, are the parameters being sent in OK.
 */
@SpringBootTest
@AutoConfigureMockMvc
class EmailControllerTests {

  @Test
  void testGetStatusOfUser(@Autowired MockMvc mvc) throws Exception {
    mvc.perform(get("/email/StephenJohnLimb@mail.com")).andExpect(status().isOk());
  }

  @Test
  void testGetStatusNotFound(@Autowired MockMvc mvc) throws Exception {
    mvc.perform(get("/email")).andExpect(status().is(404));
  }

  @Test
  void testGetStatusNotFoundEmpty(@Autowired MockMvc mvc) throws Exception {
    mvc.perform(get("/email/")).andExpect(status().is(404));
  }

  @Test
  void testGetStatusNotFoundBlank(@Autowired MockMvc mvc) throws Exception {
    mvc.perform(get("/email/ ")).andExpect(status().is(412));
  }
}
