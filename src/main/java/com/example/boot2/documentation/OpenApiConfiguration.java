package com.example.boot2.documentation;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.info.BuildProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * The details for the Swagger/OpenAPI documentation.
 */
@Configuration
public class OpenApiConfiguration {

  /**
   * Create an OpenAPI bean with all the relevant details in.
   * This can then be displayed in the generated documentation.
   */
  @Bean
  public OpenAPI customOpenApi(@Autowired BuildProperties buildProperties,
                               @Value("${apiTitle}") String apiTitle,
                               @Value("${apiDescription}") String apiDescription,
                               @Value("${apiContactName}") String apiContactName,
                               @Value("${apiContactEmail}") String apiContactEmail) {
    return new OpenAPI()
        .components(new Components())
        .info(new Info()
            .title(apiTitle).description(apiDescription).version(buildProperties.getVersion())
            .contact(new Contact().name(apiContactName).email(apiContactEmail)));
  }
}
