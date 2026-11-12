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
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import javax.inject.Inject;
import com.hubspot.baragon.auth.NoAuth;
import com.hubspot.baragon.cache.BaragonStateCache;
import com.hubspot.baragon.models.BaragonServiceState;
import com.hubspot.baragon.service.exceptions.BaragonWebException;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping(value = "/service-lookup", produces = "application/json")
public class ServiceLookupResource {
  private final ObjectMapper objectMapper;
  private final BaragonStateCache stateCache;

  @Inject
  public ServiceLookupResource(ObjectMapper objectMapper, BaragonStateCache stateCache) {
    this.objectMapper = objectMapper;
    this.stateCache = stateCache;
  }

  @GetMapping
  @NoAuth
  public List<BaragonServiceState> getServices(@RequestParam(name = "hostPort", required = false) String hostPort) {
    // Important - Normally, only a single service is on a given host and port. But in the past we've seen a critsit
    // where multiple services were on the same host and port. This endpoint returns a collection so we can easily
    // identify that scenario occur in the future.
    try {
      Collection<BaragonServiceState> services = objectMapper.readValue(
        stateCache.getState().getUncompressed(),
        new TypeReference<Collection<BaragonServiceState>>() {}
      );
      return services
        .stream()
        .filter(
          state ->
            state.getUpstreams().stream().anyMatch(u -> u.getUpstream().equals(hostPort))
        )
        .collect(Collectors.toList());
    } catch (IOException e) {
      throw new BaragonWebException("Failed to read service data.");
    }
  }
}
