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
import javax.inject.Named;
import com.hubspot.baragon.auth.NoAuth;
import com.hubspot.baragon.service.BaragonServiceConfiguration;
import com.hubspot.baragon.service.config.BaragonConfiguration;
import com.hubspot.baragon.service.views.IndexView;
import javax.inject.Singleton;

/**
 * Serves as the base for the UI, returns the mustache view for the actual GUI.
 */
@Singleton
@NoAuth
@RestController
@RequestMapping(value = UIResource.UI_RESOURCE_LOCATION + "{uiPath:.*}")
public class UIResource {
  static final String UI_RESOURCE_LOCATION = "/ui";

  private final BaragonConfiguration configuration;
  private final String baragonUriBase;

  @Inject
  public UIResource(
    @Named(BaragonServiceConfiguration.BARAGON_URI_BASE) String baragonUriBase,
    BaragonConfiguration configuration
  ) {
    this.configuration = configuration;
    this.baragonUriBase = baragonUriBase;
  }

  @GetMapping(produces = "text/html")
  public IndexView getIndex() {
    return new IndexView(baragonUriBase, UI_RESOURCE_LOCATION, configuration);
  }
}
