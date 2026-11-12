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
import com.hubspot.baragon.models.BaragonConfigFile;
import com.hubspot.baragon.service.managers.RenderedConfigsManager;
import java.util.List;

@RestController
@RequestMapping(value = "/renderedConfigs", produces = "application/json")
public class RenderedConfigsResource {
  private final RenderedConfigsManager renderedConfigsManager;

  @Inject
  public RenderedConfigsResource(RenderedConfigsManager renderedConfigsManager) {
    this.renderedConfigsManager = renderedConfigsManager;
  }

  @GetMapping(value = "/{serviceId}")
  @NoAuth
  public List<BaragonConfigFile> getRenderedConfigs(
    @PathVariable("serviceId") String serviceId
  )
    throws Exception {
    return renderedConfigsManager.synchronouslySendRenderConfigsRequest(serviceId);
  }
}
