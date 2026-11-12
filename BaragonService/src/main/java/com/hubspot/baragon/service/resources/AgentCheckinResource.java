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
import com.hubspot.baragon.auth.NoAuth;
import com.hubspot.baragon.data.BaragonLoadBalancerDatastore;
import com.hubspot.baragon.models.AgentCheckInResponse;
import com.hubspot.baragon.models.BaragonAgentMetadata;
import com.hubspot.baragon.models.BaragonGroup;
import com.hubspot.baragon.models.TrafficSourceState;
import com.hubspot.baragon.service.gcloud.GoogleCloudManager;
import com.hubspot.baragon.service.managers.ElbManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping(consumes = "application/json", value = "/checkin", produces = "application/json")
public class AgentCheckinResource {
  private static final Logger LOG = LoggerFactory.getLogger(AgentCheckinResource.class);

  private final ElbManager elbManager;
  private final GoogleCloudManager googleCloudManager;
  private final BaragonLoadBalancerDatastore loadBalancerDatastore;

  @Inject
  public AgentCheckinResource(
    ElbManager elbManager,
    GoogleCloudManager googleCloudManager,
    BaragonLoadBalancerDatastore loadBalancerDatastore
  ) {
    this.elbManager = elbManager;
    this.googleCloudManager = googleCloudManager;
    this.loadBalancerDatastore = loadBalancerDatastore;
  }

  @PostMapping(value = "/{clusterName}/startup")
  public AgentCheckInResponse addAgent(
    @PathVariable("clusterName") String clusterName,
    @RequestParam(name = "status", required = false, defaultValue = "false") boolean status,
    @RequestBody BaragonAgentMetadata agent
  ) {
    LOG.info(String.format("Notified of startup for agent %s", agent.getAgentId()));
    AgentCheckInResponse response;
    try {
      if (elbManager.isElbConfigured()) {
        response =
          elbManager.attemptAddAgent(
            agent,
            loadBalancerDatastore.getLoadBalancerGroup(clusterName),
            clusterName,
            status
          );
      } else if (googleCloudManager.isConfigured()) {
        response = googleCloudManager.checkHealthOfAgentOnStartup(agent);
      } else {
        response =
          new AgentCheckInResponse(TrafficSourceState.DONE, Optional.absent(), 0L);
      }
    } catch (Exception e) {
      LOG.error("Could not register agent startup", e);
      response =
        new AgentCheckInResponse(
          TrafficSourceState.ERROR,
          Optional.of(e.getMessage()),
          0L
        );
    }
    return response;
  }

  @PostMapping(value = "/{clusterName}/shutdown")
  public AgentCheckInResponse removeAgent(
    @PathVariable("clusterName") String clusterName,
    @RequestParam(name = "status", required = false, defaultValue = "false") boolean status,
    @RequestBody BaragonAgentMetadata agent
  ) {
    LOG.info(String.format("Notified of shutdown for agent %s", agent.getAgentId()));
    AgentCheckInResponse response;
    try {
      if (elbManager.isElbConfigured()) {
        response =
          elbManager.attemptRemoveAgent(
            agent,
            loadBalancerDatastore.getLoadBalancerGroup(clusterName),
            clusterName,
            status
          );
      } else if (googleCloudManager.isConfigured()) {
        response = googleCloudManager.checkHealthOfAgentOnShutdown(agent);
      } else {
        response =
          new AgentCheckInResponse(TrafficSourceState.DONE, Optional.absent(), 0L);
      }
    } catch (Exception e) {
      LOG.error("Could not register agent shutdown", e);
      response =
        new AgentCheckInResponse(
          TrafficSourceState.ERROR,
          Optional.of(e.getMessage()),
          0L
        );
    }
    return response;
  }

  @GetMapping(value = "/{clusterName}/can-shutdown", produces = "text/plain")
  @NoAuth
  public String canShutdownAgent(
    @PathVariable("clusterName") String clusterName,
    @RequestParam(name = "agentId", required = false) String agentId
  ) {
    Optional<BaragonAgentMetadata> maybeAgent = loadBalancerDatastore.getAgent(
      clusterName,
      agentId
    );
    Optional<BaragonGroup> maybeGroup = loadBalancerDatastore.getLoadBalancerGroup(
      clusterName
    );
    if (maybeAgent.isPresent()) {
      if (elbManager.isElbEnabledAgent(maybeAgent.get(), maybeGroup, clusterName)) {
        if (elbManager.isActiveAndHealthy(maybeGroup, maybeAgent.get())) {
          return "0";
        }
      }
    }
    return "1";
  }
}
