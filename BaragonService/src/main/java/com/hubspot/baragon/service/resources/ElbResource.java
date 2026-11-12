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
import com.amazonaws.AmazonClientException;
import com.amazonaws.services.elasticloadbalancing.AmazonElasticLoadBalancing;
import com.amazonaws.services.elasticloadbalancing.AmazonElasticLoadBalancingClient;
import com.amazonaws.services.elasticloadbalancing.model.DeregisterInstancesFromLoadBalancerRequest;
import com.amazonaws.services.elasticloadbalancing.model.DeregisterInstancesFromLoadBalancerResult;
import com.amazonaws.services.elasticloadbalancing.model.DescribeInstanceHealthRequest;
import com.amazonaws.services.elasticloadbalancing.model.DescribeInstanceHealthResult;
import com.amazonaws.services.elasticloadbalancing.model.DescribeLoadBalancersRequest;
import com.amazonaws.services.elasticloadbalancing.model.DescribeLoadBalancersResult;
import com.amazonaws.services.elasticloadbalancing.model.Instance;
import com.amazonaws.services.elasticloadbalancing.model.InstanceState;
import com.amazonaws.services.elasticloadbalancing.model.LoadBalancerDescription;
import com.amazonaws.services.elasticloadbalancing.model.RegisterInstancesWithLoadBalancerRequest;
import com.amazonaws.services.elasticloadbalancing.model.RegisterInstancesWithLoadBalancerResult;
import com.google.common.base.Optional;
import javax.inject.Inject;
import javax.inject.Named;
import com.hubspot.baragon.auth.NoAuth;
import com.hubspot.baragon.service.BaragonServiceConfiguration;
import com.hubspot.baragon.service.config.ElbConfiguration;
import com.hubspot.baragon.service.exceptions.BaragonNotFoundException;
import com.hubspot.baragon.service.exceptions.BaragonWebException;
import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping(value = "/elbs", produces = "application/json")
public class ElbResource {
  private final AmazonElasticLoadBalancing elbClient;
  private final Optional<ElbConfiguration> config;

  @Inject
  public ElbResource(
    @Named(
      BaragonServiceConfiguration.BARAGON_AWS_ELB_CLIENT_V1
    ) AmazonElasticLoadBalancing elbClient,
    Optional<ElbConfiguration> config
  ) {
    this.elbClient = elbClient;
    this.config = config;
  }

  @GetMapping
  @NoAuth
  public List<LoadBalancerDescription> getAllElbs() {
    if (config.isPresent()) {
      return elbClient.describeLoadBalancers().getLoadBalancerDescriptions();
    } else {
      throw new BaragonWebException(
        "ElbSync and related actions are not currently enabled"
      );
    }
  }

  @GetMapping(value = "/{elbName}")
  @NoAuth
  public LoadBalancerDescription getElb(@PathVariable("elbName") String elbName) {
    if (config.isPresent()) {
      try {
        DescribeLoadBalancersRequest request = new DescribeLoadBalancersRequest(
          Arrays.asList(elbName)
        );
        DescribeLoadBalancersResult result = elbClient.describeLoadBalancers(request);
        for (LoadBalancerDescription elb : result.getLoadBalancerDescriptions()) {
          if (elb.getLoadBalancerName().equals(elbName)) {
            return elb;
          }
        }
      } catch (AmazonClientException e) {
        throw new BaragonWebException(String.format("AWS Client Error: %s", e));
      }
      throw new BaragonNotFoundException(
        String.format("ELB with name %s not found", elbName)
      );
    } else {
      throw new BaragonWebException(
        "ElbSync and related actions are not currently enabled"
      );
    }
  }

  @GetMapping(value = "/{elbName}/instances")
  @NoAuth
  public List<InstanceState> getInstancesByElb(@PathVariable("elbName") String elbName) {
    if (config.isPresent()) {
      try {
        DescribeInstanceHealthRequest request = new DescribeInstanceHealthRequest(
          elbName
        );
        DescribeInstanceHealthResult result = elbClient.describeInstanceHealth(request);
        return result.getInstanceStates();
      } catch (AmazonClientException exn) {
        throw new BaragonWebException(String.format("AWS Client Error %s", exn));
      }
    } else {
      throw new BaragonWebException(
        "ElbSync and related actions are not currently enabled"
      );
    }
  }

  @PostMapping(value = "/{elbName}/update")
  public RegisterInstancesWithLoadBalancerResult addToElb(
    @PathVariable("elbName") String elbName,
    @RequestParam(name = "instanceId", required = false) String instanceId
  ) {
    if (config.isPresent()) {
      RegisterInstancesWithLoadBalancerRequest request = new RegisterInstancesWithLoadBalancerRequest(
        elbName,
        Arrays.asList(new Instance(instanceId))
      );
      return elbClient.registerInstancesWithLoadBalancer(request);
    } else {
      throw new BaragonWebException(
        "ElbSync and related actions are not currently enabled"
      );
    }
  }

  @DeleteMapping(value = "/{elbName}/update")
  public DeregisterInstancesFromLoadBalancerResult removeFromElb(
    @PathVariable("elbName") String elbName,
    @RequestParam(name = "instanceId", required = false) String instanceId
  ) {
    if (config.isPresent()) {
      DeregisterInstancesFromLoadBalancerRequest request = new DeregisterInstancesFromLoadBalancerRequest(
        elbName,
        Arrays.asList(new Instance(instanceId))
      );
      return elbClient.deregisterInstancesFromLoadBalancer(request);
    } else {
      throw new BaragonWebException(
        "ElbSync and related actions are not currently enabled"
      );
    }
  }
}
