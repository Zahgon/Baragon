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
import com.google.common.collect.Lists;
import javax.inject.Inject;
import com.hubspot.baragon.auth.NoAuth;
import com.hubspot.baragon.data.BaragonAliasDatastore;
import com.hubspot.baragon.data.BaragonKnownAgentsDatastore;
import com.hubspot.baragon.data.BaragonLoadBalancerDatastore;
import com.hubspot.baragon.data.BaragonStateDatastore;
import com.hubspot.baragon.models.BaragonAgentMetadata;
import com.hubspot.baragon.models.BaragonGroup;
import com.hubspot.baragon.models.BaragonKnownAgentMetadata;
import com.hubspot.baragon.models.BaragonService;
import com.hubspot.baragon.models.RegisterBy;
import com.hubspot.baragon.models.TrafficSource;
import com.hubspot.baragon.models.TrafficSourceType;
import com.hubspot.baragon.service.config.BaragonConfiguration;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

@RestController
@RequestMapping(consumes = "application/json", value = "/load-balancer", produces = "application/json")
public class LoadBalancerResource {
  private final BaragonLoadBalancerDatastore loadBalancerDatastore;
  private final BaragonKnownAgentsDatastore knownAgentsDatastore;
  private final BaragonStateDatastore stateDatastore;
  private final BaragonConfiguration configuration;
  private final BaragonAliasDatastore aliasDatastore;

  @Inject
  public LoadBalancerResource(
    BaragonLoadBalancerDatastore loadBalancerDatastore,
    BaragonKnownAgentsDatastore knownAgentsDatastore,
    BaragonStateDatastore stateDatastore,
    BaragonConfiguration configuration,
    BaragonAliasDatastore aliasDatastore
  ) {
    this.loadBalancerDatastore = loadBalancerDatastore;
    this.knownAgentsDatastore = knownAgentsDatastore;
    this.stateDatastore = stateDatastore;
    this.configuration = configuration;
    this.aliasDatastore = aliasDatastore;
  }

  @GetMapping
  @NoAuth
  public Collection<String> getClusters() {
    Set<String> allValidGroups = new HashSet<>();
    allValidGroups.addAll(loadBalancerDatastore.getLoadBalancerGroupNames());
    allValidGroups.addAll(aliasDatastore.getAllAliases());
    return allValidGroups;
  }

  @GetMapping(value = "/all")
  @NoAuth
  public Collection<BaragonGroup> getAllClusterDetails() {
    return loadBalancerDatastore.getLoadBalancerGroups();
  }

  @GetMapping(value = "/{clusterName}")
  @NoAuth
  public Optional<BaragonGroup> getGroupDetail(
    @PathVariable("clusterName") String clusterName
  ) {
    return loadBalancerDatastore.getLoadBalancerGroup(clusterName);
  }

  @PostMapping(value = "/{clusterName}/sources")
  @Deprecated
  public BaragonGroup addSource(
    @PathVariable("clusterName") String clusterName,
    @RequestParam(name = "source", required = false) String source
  ) {
    TrafficSource trafficSource = new TrafficSource(
      source,
      TrafficSourceType.CLASSIC,
      RegisterBy.INSTANCE_ID
    );
    return loadBalancerDatastore.addSourceToGroup(clusterName, trafficSource);
  }

  @DeleteMapping(value = "/{clusterName}/sources")
  @Deprecated
  public Optional<BaragonGroup> removeSource(
    @PathVariable("clusterName") String clusterName,
    @RequestParam(name = "source", required = false) String source
  ) {
    return loadBalancerDatastore.removeSourceFromGroup(
      clusterName,
      new TrafficSource(source, TrafficSourceType.CLASSIC, RegisterBy.INSTANCE_ID)
    );
  }

  @PostMapping(value = "/{clusterName}/traffic-source")
  public BaragonGroup addTrafficSource(
    @PathVariable("clusterName") String clusterName,
    @RequestBody @NotNull @Valid TrafficSource trafficSource
  ) {
    return loadBalancerDatastore.addSourceToGroup(clusterName, trafficSource);
  }

  @DeleteMapping(value = "/{clusterName}/traffic-source")
  public Optional<BaragonGroup> removeTrafficSource(
    @PathVariable("clusterName") String clusterName,
    @RequestBody @NotNull @Valid TrafficSource trafficSource
  ) {
    return loadBalancerDatastore.removeSourceFromGroup(clusterName, trafficSource);
  }

  @PostMapping(value = "/{clusterName}/count")
  public Integer setTargetCount(
    @PathVariable("clusterName") String clusterName,
    @RequestParam(name = "count", required = false, defaultValue = "0") int count
  ) {
    return loadBalancerDatastore.setTargetCount(clusterName, count);
  }

  @GetMapping(value = "/{clusterName}/count")
  @NoAuth
  public Integer getTargetCount(@PathVariable("clusterName") String clusterName) {
    return loadBalancerDatastore
      .getTargetCount(clusterName)
      .or(configuration.getDefaultTargetAgentCount());
  }

  @GetMapping(value = "/{clusterName}/hosts")
  @NoAuth
  @Deprecated
  public Collection<String> getHosts(@PathVariable("clusterName") String clusterName) {
    final Collection<BaragonAgentMetadata> agentMetadatas = loadBalancerDatastore.getAgentMetadata(
      clusterName
    );
    final Collection<String> baseUris = Lists.newArrayListWithCapacity(
      agentMetadatas.size()
    );

    for (BaragonAgentMetadata agentMetadata : agentMetadatas) {
      baseUris.add(agentMetadata.getBaseAgentUri());
    }

    return baseUris;
  }

  @GetMapping(value = "/{clusterName}/agents")
  @NoAuth
  public Collection<BaragonAgentMetadata> getAgentMetadata(
    @PathVariable("clusterName") String clusterName
  ) {
    return loadBalancerDatastore.getAgentMetadata(clusterName);
  }

  @GetMapping(value = "/{clusterName}/known-agents")
  @NoAuth
  public Collection<BaragonKnownAgentMetadata> getKnownAgentsMetadata(
    @PathVariable("clusterName") String clusterName
  ) {
    return knownAgentsDatastore.getKnownAgentsMetadata(clusterName);
  }

  @DeleteMapping(value = "/{clusterName}/known-agents/{agentId}")
  public void deleteKnownAgent(
    @PathVariable("clusterName") String clusterName,
    @PathVariable("agentId") String agentId
  ) {
    knownAgentsDatastore.removeKnownAgent(clusterName, agentId);
  }

  @GetMapping(value = "/{clusterName}/base-path/all")
  @NoAuth
  public Collection<String> getBasePaths(@PathVariable("clusterName") String clusterName) {
    return loadBalancerDatastore.getBasePaths(clusterName);
  }

  @GetMapping(value = "/{clusterName}/base-path")
  @NoAuth
  public Optional<BaragonService> getBasePathServiceId(
    @PathVariable("clusterName") String clusterName,
    @RequestParam(name = "basePath", required = false) String basePath
  ) {
    final Optional<String> maybeServiceId = loadBalancerDatastore.getBasePathServiceId(
      clusterName,
      basePath
    );

    if (!maybeServiceId.isPresent()) {
      return Optional.absent();
    }

    return stateDatastore.getService(maybeServiceId.get());
  }

  @DeleteMapping(value = "/{clusterName}/base-path")
  public void clearBasePath(
    @PathVariable("clusterName") String clusterName,
    @RequestParam(name = "basePath", required = false) String basePath
  ) {
    loadBalancerDatastore.clearBasePath(clusterName, basePath);
  }
}
