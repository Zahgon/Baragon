package com.hubspot.baragon.service;

import com.hubspot.baragon.service.web.WebConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.MultipartAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.error.ErrorMvcAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * The application the {@code server} command starts: the API's handlers, the components they are
 * built from and the connectors they answer on. The object mapper and the error pages are the
 * service's own, so the ones the framework would supply are left out.
 */
@Configuration
@EnableAutoConfiguration(
  exclude = {
    JacksonAutoConfiguration.class,
    ErrorMvcAutoConfiguration.class,
    MultipartAutoConfiguration.class
  }
)
@Import(
  { ObjectMapperConfiguration.class, BaragonServiceConfiguration.class, WebConfiguration.class }
)
@ComponentScan(basePackages = "com.hubspot.baragon.service.resources")
public class BaragonApplication {}
