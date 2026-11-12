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
import com.hubspot.baragon.auth.NoAuth;
import com.hubspot.baragon.models.BaragonServiceStatus;
import com.hubspot.baragon.service.managers.StatusManager;

@NoAuth
@RestController
@RequestMapping(value = "/status", produces = "application/json")
public class StatusResource {
  private final StatusManager manager;

  @Inject
  public StatusResource(StatusManager manager) {
    this.manager = manager;
  }

  @GetMapping
  public BaragonServiceStatus getServiceStatus() {
    return manager.getServiceStatus();
  }

  @GetMapping(value = "/master")
  public BaragonServiceStatus getMasterServiceStatus() {
    return manager.getMasterServiceStatus();
  }
}
