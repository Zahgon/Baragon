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
import com.google.common.base.Optional;
import javax.inject.Inject;
import com.hubspot.baragon.data.BaragonAliasDatastore;
import com.hubspot.baragon.models.BaragonGroupAlias;

@RestController
@RequestMapping(consumes = "application/json", value = "/aliases", produces = "application/json")
public class AliasesResource {
  private final BaragonAliasDatastore aliasDatastore;

  @Inject
  public AliasesResource(BaragonAliasDatastore aliasDatastore) {
    this.aliasDatastore = aliasDatastore;
  }

  @PostMapping(value = "/{name}")
  public BaragonGroupAlias createAlias(
    @PathVariable("name") String name,
    @RequestBody BaragonGroupAlias alias
  ) {
    aliasDatastore.saveAlias(name, alias);
    return alias;
  }

  @GetMapping(value = "/{name}")
  public Optional<BaragonGroupAlias> getAlias(@PathVariable("name") String name) {
    return aliasDatastore.getAlias(name);
  }

  @DeleteMapping(value = "/{name}")
  public void deleteAlias(@PathVariable("name") String name) {
    aliasDatastore.deleteAlias(name);
  }
}
