package com.hubspot.baragon.service.resources;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.hubspot.baragon.service.platform.http.MediaType;
import com.codahale.metrics.MetricRegistry;
import javax.inject.Inject;

@RestController
@RequestMapping(value = MetricsResource.PATH, produces = "application/json")
public class MetricsResource {
  public static final String PATH = "/metrics";

  private final MetricRegistry registry;

  @Inject
  public MetricsResource(MetricRegistry registry) {
    this.registry = registry;
  }

  @GetMapping
  public MetricRegistry getRegistry() {
    return registry;
  }
}
