package com.example.boot2;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

/**
 * This is designed to only test documentation URLS (not really their content).
 */
@SpringBootTest
@AutoConfigureMockMvc
class Boot2OpenAPITests {

  @Test
  void testGetOpenAPIDocumentationPage(@Autowired MockMvc mvc) throws Exception {
    mvc.perform(get("/api-docs")).andExpect(status().isOk());
  }

  @Test
  void testGetOpenAPIDocumentationYAMLPage(@Autowired MockMvc mvc) throws Exception {
    mvc.perform(get("/api-docs.yaml")).andExpect(status().isOk());
  }

  @Test
  void testGetOpenAPIEndUserDocumentation(@Autowired MockMvc mvc) throws Exception {
    mvc.perform(get("/api-ui.html"))
        .andExpect(status().is(302))
        .andExpect(redirectedUrl("/swagger-ui/index.html"));
  }
}
