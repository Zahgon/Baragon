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
import javax.inject.Inject;
import com.hubspot.baragon.models.BaragonResponse;
import com.hubspot.baragon.service.managers.PurgeCacheManager;
import com.hubspot.baragon.service.managers.ServiceManager;

@RestController
@RequestMapping(value = "/purgeCache", produces = "application/json")
public class PurgeCacheResource {
  private final PurgeCacheManager purgeCacheManager;
  private final ServiceManager serviceManager;

  @Inject
  public PurgeCacheResource(
    PurgeCacheManager purgeCacheManager,
    ServiceManager serviceManager
  ) {
    this.purgeCacheManager = purgeCacheManager;
    this.serviceManager = serviceManager;
  }

  @PostMapping(value = "/{serviceId}")
  public BaragonResponse purgeCacheAsync(@PathVariable("serviceId") String serviceId) {
    return serviceManager.enqueuePurgeCache(serviceId);
  }
}
