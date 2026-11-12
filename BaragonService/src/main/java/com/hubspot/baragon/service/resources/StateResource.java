package com.hubspot.baragon.service.resources;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.hubspot.baragon.service.platform.http.MediaType;
import com.hubspot.baragon.service.platform.http.Response;
import com.hubspot.baragon.service.platform.http.Response.ResponseBuilder;
import com.codahale.metrics.annotation.Timed;
import com.google.common.base.Optional;
import com.google.common.net.HttpHeaders;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import com.hubspot.baragon.auth.NoAuth;
import com.hubspot.baragon.cache.BaragonStateCache;
import com.hubspot.baragon.cache.CachedBaragonState;
import com.hubspot.baragon.models.BaragonResponse;
import com.hubspot.baragon.models.BaragonServiceState;
import com.hubspot.baragon.service.managers.ServiceManager;

@RestController
@RequestMapping(value = "/state", produces = "application/json")
public class StateResource {
  private final ServiceManager serviceManager;
  private final BaragonStateCache stateCache;

  @Inject
  public StateResource(ServiceManager serviceManager, BaragonStateCache stateCache) {
    this.serviceManager = serviceManager;
    this.stateCache = stateCache;
  }

  @GetMapping
  @NoAuth
  @Timed
  public Response getAllServices(HttpServletRequest request) {
    // A header the API reads as a value holds the first value the request carries under that name,
    // as it arrived: a request that names the header more than once is read by its first, and a
    // value that holds a list is read whole.
    String ifNoneMatch = request.getHeader(HttpHeaders.IF_NONE_MATCH);
    String acceptEncoding = request.getHeader(HttpHeaders.ACCEPT_ENCODING);
    CachedBaragonState state = stateCache.getState();
    String verisonString = Integer.toString(state.getVersion());

    if (ifNoneMatch != null && ifNoneMatch.equals(verisonString.trim())) {
      return Response.notModified().header(HttpHeaders.ETAG, state.getVersion()).build();
    }

    ResponseBuilder builder = Response.ok();

    final byte[] entity;
    if (acceptEncoding != null && acceptEncoding.contains("gzip")) {
      builder.header(HttpHeaders.CONTENT_ENCODING, "gzip");
      entity = state.getGzip();
    } else {
      entity = state.getUncompressed();
    }

    return builder.entity(entity).header(HttpHeaders.ETAG, state.getVersion()).build();
  }

  @GetMapping(value = "/{serviceId}")
  @NoAuth
  public Optional<BaragonServiceState> getService(
    @PathVariable("serviceId") String serviceId
  ) {
    return serviceManager.getService(serviceId);
  }

  @PostMapping(value = "/{serviceId}/reload")
  public BaragonResponse reloadConfigs(
    @PathVariable("serviceId") String serviceId,
    @RequestParam(name = "noValidate", required = false, defaultValue = "false") boolean noValidate
  ) {
    return serviceManager.enqueueReloadServiceConfigs(serviceId, noValidate);
  }

  @DeleteMapping(value = "/{serviceId}")
  public BaragonResponse removeService(
    @PathVariable("serviceId") String serviceId,
    @RequestParam(name = "noValidate", required = false, defaultValue = "false") boolean noValidate,
    @RequestParam(name = "noReload", required = false, defaultValue = "false") boolean noReload
  ) {
    return serviceManager.enqueueRemoveService(serviceId, noValidate, noReload);
  }
}
