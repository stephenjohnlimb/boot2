package com.example.boot2;

import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class Boot2ApplicationTests {

  @Test
  void justFail() {
    fail("Just checking");
  }

  @Test
  void contextLoads() {
  }

}
