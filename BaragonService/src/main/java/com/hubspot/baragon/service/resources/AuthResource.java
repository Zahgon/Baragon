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
import com.hubspot.baragon.service.platform.http.Response;
import com.hubspot.baragon.service.platform.http.Response.Status;
import com.hubspot.baragon.service.platform.http.WebApplicationException;
import com.google.common.base.Optional;
import javax.inject.Inject;
import javax.inject.Named;
import com.hubspot.baragon.auth.NoAuth;
import com.hubspot.baragon.data.BaragonAuthDatastore;
import com.hubspot.baragon.models.BaragonAuthKey;
import com.hubspot.baragon.service.BaragonServiceConfiguration;
import java.util.Collection;

@RestController
@RequestMapping(consumes = "application/json", value = "/auth", produces = "application/json")
public class AuthResource {
  private final BaragonAuthDatastore datastore;
  private final String masterAuthKey;

  @Inject
  public AuthResource(
    BaragonAuthDatastore datastore,
    @Named(BaragonServiceConfiguration.BARAGON_MASTER_AUTH_KEY) String masterAuthKey
  ) {
    this.datastore = datastore;
    this.masterAuthKey = masterAuthKey;
  }

  @GetMapping(value = "/key/verify")
  public void verifyKey() {}

  @GetMapping(value = "/keys")
  @NoAuth
  public Collection<BaragonAuthKey> getKeys(@RequestParam(name = "authkey", required = false) String queryAuthKey) {
    if (!masterAuthKey.equals(queryAuthKey)) {
      throw new WebApplicationException(Response.status(Status.FORBIDDEN).build());
    }

    return datastore.getAuthKeyMap().values();
  }

  @DeleteMapping(value = "/keys/{key}")
  @NoAuth
  public Optional<BaragonAuthKey> expireKey(
    @PathVariable("key") String key,
    @RequestParam(name = "authkey", required = false) String queryAuthKey
  ) {
    if (!masterAuthKey.equals(queryAuthKey)) {
      throw new WebApplicationException(Response.status(Status.FORBIDDEN).build());
    }

    return datastore.expireAuthKey(key);
  }

  @PostMapping(value = "/keys")
  @NoAuth
  public void addKey(@RequestBody BaragonAuthKey authKey, @RequestParam(name = "authkey", required = false) String queryAuthKey) {
    if (!masterAuthKey.equals(queryAuthKey)) {
      throw new WebApplicationException(Response.status(Status.FORBIDDEN).build());
    }

    datastore.addAuthKey(authKey);
  }
}
