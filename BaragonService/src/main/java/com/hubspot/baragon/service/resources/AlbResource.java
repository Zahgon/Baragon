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
import com.amazonaws.AmazonClientException;
import com.amazonaws.services.elasticloadbalancingv2.model.CreateListenerRequest;
import com.amazonaws.services.elasticloadbalancingv2.model.CreateRuleRequest;
import com.amazonaws.services.elasticloadbalancingv2.model.CreateTargetGroupRequest;
import com.amazonaws.services.elasticloadbalancingv2.model.Listener;
import com.amazonaws.services.elasticloadbalancingv2.model.ListenerNotFoundException;
import com.amazonaws.services.elasticloadbalancingv2.model.LoadBalancer;
import com.amazonaws.services.elasticloadbalancingv2.model.ModifyListenerRequest;
import com.amazonaws.services.elasticloadbalancingv2.model.ModifyRuleRequest;
import com.amazonaws.services.elasticloadbalancingv2.model.ModifyTargetGroupRequest;
import com.amazonaws.services.elasticloadbalancingv2.model.Rule;
import com.amazonaws.services.elasticloadbalancingv2.model.RuleNotFoundException;
import com.amazonaws.services.elasticloadbalancingv2.model.TargetGroup;
import com.amazonaws.services.elasticloadbalancingv2.model.TargetGroupNotFoundException;
import com.amazonaws.services.elasticloadbalancingv2.model.TargetHealthDescription;
import com.google.common.base.Optional;
import javax.inject.Inject;
import com.hubspot.baragon.auth.NoAuth;
import com.hubspot.baragon.models.AgentCheckInResponse;
import com.hubspot.baragon.service.config.ElbConfiguration;
import com.hubspot.baragon.service.elb.ApplicationLoadBalancer;
import com.hubspot.baragon.service.exceptions.BaragonWebException;
import java.util.Collection;
import javax.validation.Valid;

@RestController
@RequestMapping(value = "/albs", produces = "application/json")
public class AlbResource {
  private final ApplicationLoadBalancer applicationLoadBalancer;
  private final Optional<ElbConfiguration> config;

  @Inject
  public AlbResource(
    ApplicationLoadBalancer applicationLoadBalancer,
    Optional<ElbConfiguration> config
  ) {
    this.applicationLoadBalancer = applicationLoadBalancer;
    this.config = config;
  }

  @GetMapping(value = "/load-balancers")
  @NoAuth
  public Collection<LoadBalancer> getAllLoadBalancers() {
    if (config.isPresent()) {
      return applicationLoadBalancer.getAllLoadBalancers();
    } else {
      throw new BaragonWebException(
        "ElbSync and related actions are not currently enabled"
      );
    }
  }

  @GetMapping(value = "/load-balancers/{elbName}")
  @NoAuth
  public LoadBalancer getLoadBalancer(@PathVariable("elbName") String elbName) {
    if (config.isPresent()) {
      try {
        Optional<LoadBalancer> maybeLoadBalancer = applicationLoadBalancer.getLoadBalancer(
          elbName
        );
        if (maybeLoadBalancer.isPresent()) {
          return maybeLoadBalancer.get();
        } else {
          throw new WebApplicationException(
            String.format("ELB %s not found", elbName),
            Status.NOT_FOUND
          );
        }
      } catch (AmazonClientException exn) {
        throw new BaragonWebException(String.format("AWS client exception %s", exn));
      }
    } else {
      throw new BaragonWebException(
        "ElbSync and related actions are not currently enabled"
      );
    }
  }

  @GetMapping(value = "/load-balancers/{elbName}/listeners")
  @NoAuth
  public Collection<Listener> getListeners(@PathVariable("elbName") String elbName) {
    if (config.isPresent()) {
      try {
        return applicationLoadBalancer.getListenersForElb(elbName);
      } catch (AmazonClientException exn) {
        throw new BaragonWebException(String.format("AWS client exception %s", exn));
      }
    } else {
      throw new BaragonWebException(
        "ElbSync and related actions are not currently enabled"
      );
    }
  }

  @PostMapping(value = "/load-balancers/{elbName}/listeners")
  public Listener createListeners(
    @PathVariable("elbName") String elbName,
    @RequestBody @Valid CreateListenerRequest createListenerRequest
  ) {
    if (config.isPresent()) {
      Optional<LoadBalancer> maybeLoadBalancer = applicationLoadBalancer.getLoadBalancer(
        elbName
      );
      if (maybeLoadBalancer.isPresent()) {
        return applicationLoadBalancer.createListener(
          createListenerRequest.withLoadBalancerArn(
            maybeLoadBalancer.get().getLoadBalancerArn()
          )
        );
      } else {
        throw new WebApplicationException(
          String.format("Could not find an elb with name %s", elbName),
          Status.NOT_FOUND
        );
      }
    } else {
      throw new BaragonWebException(
        "ElbSync and related actions are not currently enabled"
      );
    }
  }

  @PostMapping(value = "/listeners/{listenerArn:.+}")
  public Listener updateListener(
    @PathVariable("elbName") String elbName,
    @PathVariable("listenerArn") String listenerArn,
    @RequestBody @Valid ModifyListenerRequest modifyListenerRequest
  ) {
    if (config.isPresent()) {
      try {
        return applicationLoadBalancer.modifyListener(
          modifyListenerRequest.withListenerArn(listenerArn)
        );
      } catch (ListenerNotFoundException notFound) {
        throw new WebApplicationException(
          String.format("No listener with ARN %s found", listenerArn),
          Status.NOT_FOUND
        );
      } catch (AmazonClientException exn) {
        throw new BaragonWebException(String.format("AWS client exception %s", exn));
      }
    } else {
      throw new BaragonWebException(
        "ElbSync and related actions are not currently enabled"
      );
    }
  }

  @DeleteMapping(value = "/listeners/{listenerArn:.+}")
  public Response removeListener(
    @PathVariable("elbName") String elbName,
    @PathVariable("listenerArn") String listenerArn
  ) {
    if (config.isPresent()) {
      try {
        applicationLoadBalancer.deleteListener(listenerArn);
        return Response.noContent().build();
      } catch (ListenerNotFoundException notFound) {
        throw new WebApplicationException(
          String.format("No listener with ARN %s found", listenerArn),
          Status.NOT_FOUND
        );
      } catch (AmazonClientException exn) {
        throw new BaragonWebException(String.format("AWS client exception %s", exn));
      }
    } else {
      throw new BaragonWebException(
        "ElbSync and related actions are not currently enabled"
      );
    }
  }

  @GetMapping(value = "/listeners/rules/{listenerArn:.+}")
  @NoAuth
  public Collection<Rule> getRules(
    @PathVariable("elbName") String elbName,
    @PathVariable("listenerArn") String listenerArn
  ) {
    if (config.isPresent()) {
      try {
        return applicationLoadBalancer.getRulesByListener(listenerArn);
      } catch (ListenerNotFoundException notFound) {
        throw new WebApplicationException(
          String.format("Listener with ARN %s not found", listenerArn),
          Status.NOT_FOUND
        );
      } catch (AmazonClientException exn) {
        throw new BaragonWebException(String.format("AWS client exception %s", exn));
      }
    } else {
      throw new BaragonWebException(
        "ElbSync and related actions are not currently enabled"
      );
    }
  }

  @PostMapping(value = "/listeners/rules/{listenerArn:.+}")
  public Rule createRule(
    @PathVariable("elbName") String elbName,
    @PathVariable("listenerArn") String listenerArn,
    @RequestBody @Valid CreateRuleRequest createRuleRequest
  ) {
    if (config.isPresent()) {
      try {
        return applicationLoadBalancer.createRule(
          createRuleRequest.withListenerArn(listenerArn)
        );
      } catch (ListenerNotFoundException notFound) {
        throw new WebApplicationException(
          String.format("Listener with ARN %s not found", listenerArn),
          Status.NOT_FOUND
        );
      } catch (AmazonClientException exn) {
        throw new BaragonWebException(String.format("AWS client exception %s", exn));
      }
    } else {
      throw new BaragonWebException(
        "ElbSync and related actions are not currently enabled"
      );
    }
  }

  @PostMapping(value = "/rules/{ruleArn:.+}")
  public Rule updateRule(
    @PathVariable("elbName") String elbName,
    @PathVariable("listenerArn") String listenerArn,
    @PathVariable("ruleArn") String ruleArn,
    @RequestBody @Valid ModifyRuleRequest modifyRuleRequest
  ) {
    if (config.isPresent()) {
      try {
        return applicationLoadBalancer.modifyRule(modifyRuleRequest.withRuleArn(ruleArn));
      } catch (RuleNotFoundException notFound) {
        throw new WebApplicationException(
          String.format("Rule with ARN %s found", ruleArn),
          Status.NOT_FOUND
        );
      } catch (AmazonClientException exn) {
        throw new BaragonWebException(String.format("AWS client exception %s", exn));
      }
    } else {
      throw new BaragonWebException(
        "ElbSync and related actions are not currently enabled"
      );
    }
  }

  @DeleteMapping(value = "/rules/{ruleArn:.+}")
  public Response deleteRule(
    @PathVariable("elbName") String elbName,
    @PathVariable("listenerArn") String listenerArn,
    @PathVariable("ruleArn") String ruleArn
  ) {
    if (config.isPresent()) {
      try {
        applicationLoadBalancer.deleteRule(ruleArn);
        return Response.noContent().build();
      } catch (RuleNotFoundException notFound) {
        throw new WebApplicationException(
          String.format("Rule with ARN %s not found", ruleArn),
          Status.NOT_FOUND
        );
      } catch (AmazonClientException exn) {
        throw new BaragonWebException(String.format("Amazon client exception %s", exn));
      }
    } else {
      throw new BaragonWebException(
        "ElbSync and related actions are not currently enabled"
      );
    }
  }

  @GetMapping(value = "/target-groups")
  @NoAuth
  public Collection<TargetGroup> getAllTargetGroups() {
    if (config.isPresent()) {
      return applicationLoadBalancer.getAllTargetGroups();
    } else {
      throw new BaragonWebException(
        "ElbSync and related actions are not currently enabled"
      );
    }
  }

  @PostMapping
  // the repository names a path parameter here rather than a path, so this answers on the class's path
  public TargetGroup createTargetGroup(
    @RequestBody @Valid CreateTargetGroupRequest createTargetGroupRequest
  ) {
    if (config.isPresent()) {
      try {
        return applicationLoadBalancer.createTargetGroup(createTargetGroupRequest);
      } catch (AmazonClientException exn) {
        throw new BaragonWebException(String.format("AWS client exception %s", exn));
      }
    } else {
      throw new BaragonWebException(
        "ElbSync and related actions are not currently enabled"
      );
    }
  }

  @GetMapping(value = "/target-groups/{targetGroup}")
  @NoAuth
  public TargetGroup getTargetGroup(@PathVariable("targetGroup") String targetGroup) {
    if (config.isPresent()) {
      try {
        Optional<TargetGroup> maybeTargetGroup = applicationLoadBalancer.getTargetGroup(
          targetGroup
        );
        if (maybeTargetGroup.isPresent()) {
          return maybeTargetGroup.get();
        } else {
          throw new WebApplicationException(
            String.format("TargetGroup with name %s not found", targetGroup),
            Status.NOT_FOUND
          );
        }
      } catch (AmazonClientException exn) {
        throw new BaragonWebException(String.format("AWS Client exception %s", exn));
      }
    } else {
      throw new BaragonWebException(
        "ElbSync and related actions are not currently enabled"
      );
    }
  }

  @PostMapping(value = "/target-groups/{targetGroup}")
  public TargetGroup modifyTargetGroup(
    @PathVariable("targetGroup") String targetGroup,
    @RequestBody @Valid ModifyTargetGroupRequest modifyTargetGroupRequest
  ) {
    if (config.isPresent()) {
      try {
        return applicationLoadBalancer.modifyTargetGroup(modifyTargetGroupRequest);
      } catch (TargetGroupNotFoundException notFound) {
        throw new WebApplicationException(
          String.format("Target group %s not found", targetGroup),
          Status.NOT_FOUND
        );
      } catch (AmazonClientException exn) {
        throw new BaragonWebException(String.format("AWS client exception %s", exn));
      }
    } else {
      throw new BaragonWebException(
        "ElbSync and related actions are not currently enabled"
      );
    }
  }

  @GetMapping(value = "/target-groups/{targetGroup}/targets")
  @NoAuth
  public Collection<TargetHealthDescription> getTargets(
    @PathVariable("targetGroup") String targetGroup
  ) {
    if (config.isPresent()) {
      try {
        Optional<TargetGroup> maybeTargetGroup = applicationLoadBalancer.getTargetGroup(
          targetGroup
        );
        if (maybeTargetGroup.isPresent()) {
          return applicationLoadBalancer.getTargetsOn(maybeTargetGroup.get());
        } else {
          throw new WebApplicationException(
            String.format("TargetGroup with name %s not found", targetGroup),
            Status.NOT_FOUND
          );
        }
      } catch (AmazonClientException exn) {
        throw new BaragonWebException(String.format("AWS Client exception %s", exn));
      }
    } else {
      throw new BaragonWebException(
        "ElbSync and related actions are not currently enabled"
      );
    }
  }

  @DeleteMapping(value = "/target-groups/{targetGroup}/targets/{instanceId}")
  public AgentCheckInResponse removeFromTargetGroup(
    @PathVariable("targetGroup") String targetGroup,
    @PathVariable("instanceId") String instanceId
  ) {
    if (instanceId == null) {
      throw new BaragonWebException(
        "Must provide instance ID to remove target from group"
      );
    } else if (config.isPresent()) {
      AgentCheckInResponse result = applicationLoadBalancer.removeInstance(
        instanceId,
        targetGroup
      );
      if (result.getExceptionMessage().isPresent()) {
        throw new WebApplicationException(
          result.getExceptionMessage().get(),
          Status.INTERNAL_SERVER_ERROR
        );
      }
      return result;
    } else {
      throw new BaragonWebException("ElbSync and related actions not currently enabled");
    }
  }

  @PostMapping(value = "/target-group/{targetGroup}/targets")
  public AgentCheckInResponse addToTargetGroup(
    @PathVariable("targetGroup") String targetGroup,
    @RequestParam(name = "instanceId", required = false) String instanceId
  ) {
    if (instanceId == null) {
      throw new BaragonWebException("Must provide instance ID to add target to group");
    } else if (config.isPresent()) {
      try {
        return applicationLoadBalancer.registerInstance(instanceId, targetGroup);
      } catch (TargetGroupNotFoundException notFound) {
        throw new WebApplicationException(
          String.format("Target group %s not found", targetGroup),
          Status.NOT_FOUND
        );
      } catch (AmazonClientException exn) {
        throw new BaragonWebException(
          String.format(
            "Failed to add instance %s to target group %s",
            instanceId,
            targetGroup
          )
        );
      }
    } else {
      throw new BaragonWebException(
        "ElbSync and related actions are not currently enabled"
      );
    }
  }
}
