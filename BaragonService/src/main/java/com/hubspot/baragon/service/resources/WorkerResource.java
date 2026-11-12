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
import com.hubspot.baragon.data.BaragonWorkerDatastore;
import java.util.Collection;

@NoAuth
@RestController
@RequestMapping(value = "/workers", produces = "application/json")
public class WorkerResource {
  private final BaragonWorkerDatastore datastore;

  @Inject
  public WorkerResource(BaragonWorkerDatastore datastore) {
    this.datastore = datastore;
  }

  @GetMapping
  public Collection<String> getWorkers() {
    return datastore.getBaseUris();
  }
}
