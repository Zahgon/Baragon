package com.hubspot.baragon.service;

import com.amazonaws.ClientConfigurationFactory;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Regions;
import com.amazonaws.retry.PredefinedBackoffStrategies.ExponentialBackoffStrategy;
import com.amazonaws.retry.RetryPolicy;
import com.amazonaws.retry.RetryPolicy.RetryCondition;
import com.amazonaws.services.elasticloadbalancing.AmazonElasticLoadBalancing;
import com.amazonaws.services.elasticloadbalancing.AmazonElasticLoadBalancingClient;
import com.amazonaws.services.elasticloadbalancing.AmazonElasticLoadBalancingClientBuilder;
import com.codahale.metrics.MetricRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.compute.Compute;
import com.google.common.base.Optional;
import com.google.common.base.Strings;
import com.hubspot.baragon.BaragonDataModule;
import com.hubspot.baragon.config.AuthConfiguration;
import com.hubspot.baragon.config.HttpClientConfiguration;
import com.hubspot.baragon.config.ZooKeeperConfiguration;
import com.hubspot.baragon.data.BaragonConnectionStateListener;
import com.hubspot.baragon.data.BaragonWorkerDatastore;
import com.hubspot.baragon.service.config.BaragonConfiguration;
import com.hubspot.baragon.service.config.BaragonServiceDWSettings;
import com.hubspot.baragon.service.config.EdgeCacheConfiguration;
import com.hubspot.baragon.service.config.ElbConfiguration;
import com.hubspot.baragon.service.config.SentryConfiguration;
import com.hubspot.baragon.service.edgecache.EdgeCache;
import com.hubspot.baragon.service.platform.ConnectorFactory;
import com.hubspot.baragon.service.platform.Environment;
import com.hubspot.baragon.service.platform.HttpConnectorFactory;
import com.hubspot.baragon.utils.JavaUtils;
import com.hubspot.baragon.utils.UpstreamResolver;
import com.hubspot.horizon.HttpConfig;
import com.hubspot.horizon.ning.NingHttpClient;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.AsyncHttpClientConfig;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicLong;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.recipes.leader.LeaderLatch;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * What the service is made of beyond its API: the parts of the configuration file its components
 * read, the connection to the state store, the hostname and base URI it knows itself by, the leader
 * latch, the clients it talks to agents, the cloud and the edge cache with, and the scheduled
 * executor its background work runs on.
 */
@Configuration
@Import(BaragonDataConfiguration.class)
@ComponentScan(
  basePackages = {
    "com.hubspot.baragon.service.edgecache",
    "com.hubspot.baragon.service.elb",
    "com.hubspot.baragon.service.exceptions",
    "com.hubspot.baragon.service.gcloud",
    "com.hubspot.baragon.service.healthcheck",
    "com.hubspot.baragon.service.listeners",
    "com.hubspot.baragon.service.managed",
    "com.hubspot.baragon.service.managers",
    "com.hubspot.baragon.service.worker"
  }
)
public class BaragonServiceConfiguration {
  private static final Logger LOG = LoggerFactory.getLogger(
    BaragonServiceConfiguration.class
  );

  public static final String BARAGON_SERVICE_SCHEDULED_EXECUTOR =
    "baragon.service.scheduledExecutor";

  public static final String BARAGON_SERVICE_DW_CONFIG = "baragon.service.port";
  public static final String BARAGON_SERVICE_HOSTNAME = "baragon.service.hostname";
  public static final String BARAGON_SERVICE_LOCAL_HOSTNAME =
    "baragon.service.local.hostname";
  public static final String BARAGON_SERVICE_HTTP_CLIENT =
    "baragon.service.async.http.client";
  public static final String BARAGON_SERVICE_SYNC_HTTP_CLIENT =
    "baragon.service.sync.http.client";

  public static final String BARAGON_MASTER_AUTH_KEY = "baragon.master.auth.key";

  public static final String BARAGON_URI_BASE = "baragon.uri.base";

  public static final String BARAGON_AWS_ELB_CLIENT_V1 = "baragon.aws.elb.client.v1";
  public static final String BARAGON_AWS_ELB_CLIENT_V2 = "baragon.aws.elb.client.v2";

  public static final String GOOGLE_CLOUD_COMPUTE_SERVICE =
    "baragon.google.cloud.compute.service";

  private static final RetryCondition RETRY_CONDITION = (
      amazonWebServiceRequest,
      e,
      i
    ) -> {
    LOG.debug("e={}, isRetryable={}, i={}", e.getMessage(), e.isRetryable(), i);
    return e.isRetryable();
  };

  @Bean
  public ZooKeeperConfiguration provideZooKeeperConfiguration(
    BaragonConfiguration configuration
  ) {
    return configuration.getZooKeeperConfiguration();
  }

  @Bean
  public HttpClientConfiguration provideHttpClientConfiguration(
    BaragonConfiguration configuration
  ) {
    return configuration.getHttpClientConfiguration();
  }

  @Bean(name = BaragonDataModule.BARAGON_AGENT_REQUEST_URI_FORMAT)
  public String provideAgentUriFormat(BaragonConfiguration configuration) {
    return configuration.getAgentRequestUriFormat();
  }

  @Bean(name = BaragonDataModule.BARAGON_AGENT_BATCH_REQUEST_URI_FORMAT)
  public String provideAgentBatchUriFormat(BaragonConfiguration configuration) {
    return configuration.getAgentBatchRequestUriFormat();
  }

  @Bean(name = BaragonDataModule.BARAGON_AGENT_MAX_ATTEMPTS)
  public Integer provideAgentMaxAttempts(BaragonConfiguration configuration) {
    return configuration.getAgentMaxAttempts();
  }

  @Bean(name = BaragonDataModule.BARAGON_AGENT_REQUEST_TIMEOUT_MS)
  public Long provideAgentMaxRequestTime(BaragonConfiguration configuration) {
    return configuration.getAgentRequestTimeoutMs();
  }

  @Bean
  public AuthConfiguration providesAuthConfiguration(BaragonConfiguration configuration) {
    return configuration.getAuthConfiguration();
  }

  @Bean
  public Optional<ElbConfiguration> providesElbConfiguration(
    BaragonConfiguration configuration
  ) {
    return configuration.getElbConfiguration();
  }

  @Bean
  public EdgeCacheConfiguration providesEdgeCacheConfiguration(
    BaragonConfiguration configuration
  ) {
    return configuration.getEdgeCacheConfiguration();
  }

  /** Which way the service purges an edge cache, as the configuration file names it. */
  @Bean
  @org.springframework.context.annotation.Primary
  public EdgeCache providesEdgeCache(
    BaragonConfiguration configuration,
    org.springframework.beans.factory.BeanFactory beanFactory
  ) {
    return beanFactory.getBean(
      configuration.getEdgeCacheConfiguration().getEdgeCache().getEdgeCacheClass()
    );
  }

  @Bean(name = BARAGON_SERVICE_SCHEDULED_EXECUTOR)
  public ScheduledExecutorService providesScheduledExecutor() {
    return Executors.newScheduledThreadPool(5);
  }

  @Bean(name = BaragonDataModule.BARAGON_SERVICE_WORKER_LAST_START)
  public AtomicLong providesWorkerLastStartAt() {
    return new AtomicLong();
  }

  @Bean(name = BaragonDataModule.BARAGON_ELB_WORKER_LAST_START)
  public AtomicLong providesElbWorkerLastStartAt() {
    return new AtomicLong();
  }

  /**
   * The port and the root path the API answers on, read from the configuration file: the first plain
   * connector of the API is the one the service knows itself by.
   */
  @Bean(name = BARAGON_SERVICE_DW_CONFIG)
  public BaragonServiceDWSettings providesHttpPortProperty(BaragonConfiguration config) {
    Integer port = null;
    final String contextPath = config.getServerFactory().getApplicationContextPath();
    // Currently we only look for an http connector, not an https connector
    for (ConnectorFactory connectorFactory : config
      .getServerFactory()
      .getApplicationConnectors()) {
      if (connectorFactory instanceof HttpConnectorFactory) {
        port = connectorFactory.getPort();
      }
    }
    if (port == null) {
      throw new RuntimeException("Could not determine http port");
    }
    return new BaragonServiceDWSettings(port, contextPath);
  }

  @Bean(name = BARAGON_SERVICE_HOSTNAME)
  public String providesHostnameProperty(BaragonConfiguration config) throws Exception {
    return Strings.isNullOrEmpty(config.getHostname())
      ? JavaUtils.getHostAddress()
      : config.getHostname();
  }

  @Bean(name = BARAGON_SERVICE_LOCAL_HOSTNAME)
  public String providesLocalHostnameProperty(BaragonConfiguration config) {
    if (!Strings.isNullOrEmpty(config.getHostname())) {
      return config.getHostname();
    }

    try {
      final InetAddress addr = InetAddress.getLocalHost();

      return addr.getHostName();
    } catch (UnknownHostException e) {
      throw new RuntimeException(
        "No local hostname found, unable to start without functioning local networking (or configured hostname)",
        e
      );
    }
  }

  @Bean(name = BaragonDataModule.BARAGON_SERVICE_LEADER_LATCH)
  public LeaderLatch providesServiceLeaderLatch(
    BaragonConfiguration config,
    BaragonWorkerDatastore datastore,
    @javax.inject.Named(BARAGON_SERVICE_DW_CONFIG) BaragonServiceDWSettings dwConfig,
    @javax.inject.Named(BARAGON_SERVICE_HOSTNAME) String hostname
  ) {
    final String baseUri = String.format(
      "http://%s:%s%s",
      hostname,
      dwConfig.getPort(),
      dwConfig.getContextPath()
    );

    return datastore.createLeaderLatch(baseUri);
  }

  @Bean(name = BARAGON_MASTER_AUTH_KEY)
  public String providesMasterAuthKey(BaragonConfiguration configuration) {
    return Strings.nullToEmpty(configuration.getMasterAuthKey());
  }

  @Bean(name = BARAGON_URI_BASE)
  String getBaragonUriBase(
    final BaragonConfiguration configuration,
    @javax.inject.Named(BARAGON_SERVICE_DW_CONFIG) BaragonServiceDWSettings dwSettings
  ) {
    final String baragonUiPrefix = configuration
      .getUiConfiguration()
      .getBaseUrl()
      .or(dwSettings.getContextPath());
    return (baragonUiPrefix.endsWith("/"))
      ? baragonUiPrefix.substring(0, baragonUiPrefix.length() - 1)
      : baragonUiPrefix;
  }

  @Bean(name = BARAGON_SERVICE_HTTP_CLIENT)
  public AsyncHttpClient providesHttpClient(HttpClientConfiguration config) {
    AsyncHttpClientConfig.Builder builder = new AsyncHttpClientConfig.Builder();

    builder.setMaxRequestRetry(config.getMaxRequestRetry());
    builder.setRequestTimeout(config.getRequestTimeoutInMs());
    builder.setFollowRedirect(true);
    builder.setConnectTimeout(config.getConnectionTimeoutInMs());
    builder.setUserAgent(config.getUserAgent());

    return new AsyncHttpClient(builder.build());
  }

  @Bean(name = BARAGON_SERVICE_SYNC_HTTP_CLIENT)
  public NingHttpClient providesApacheHttpClient(
    HttpClientConfiguration config,
    ObjectMapper objectMapper
  ) {
    HttpConfig.Builder configBuilder = HttpConfig
      .newBuilder()
      .setRequestTimeoutSeconds(config.getRequestTimeoutInMs() / 1000)
      .setUserAgent(config.getUserAgent())
      .setConnectTimeoutSeconds(config.getConnectionTimeoutInMs() / 1000)
      .setFollowRedirects(true)
      .setMaxRetries(config.getMaxRequestRetry())
      .setObjectMapper(objectMapper);

    return new NingHttpClient(configBuilder.build());
  }

  @Bean(name = BARAGON_AWS_ELB_CLIENT_V1)
  public AmazonElasticLoadBalancing providesAwsElbClientV1(
    Optional<ElbConfiguration> configuration
  ) {
    AmazonElasticLoadBalancing elbClient;
    if (
      configuration.isPresent() &&
      configuration.get().getAwsAccessKeyId() != null &&
      configuration.get().getAwsAccessKeySecret() != null &&
      configuration.get().getAwsRegion().isPresent()
    ) {
      elbClient =
        AmazonElasticLoadBalancingClientBuilder
          .standard()
          .withCredentials(
            new AWSStaticCredentialsProvider(
              new BasicAWSCredentials(
                configuration.get().getAwsAccessKeyId(),
                configuration.get().getAwsAccessKeySecret()
              )
            )
          )
          .withClientConfiguration(
            new ClientConfigurationFactory()
              .getConfig()
              .withRetryPolicy(
                new RetryPolicy(
                  RETRY_CONDITION,
                  new ExponentialBackoffStrategy(
                    configuration
                      .or(new ElbConfiguration())
                      .getAwsElbClientBackoffBaseDelayMilliseconds(),
                    configuration
                      .or(new ElbConfiguration())
                      .getAwsElbClientBackoffMaxBackoffMilliseconds()
                  ),
                  configuration.get().getAwsElbClientRetries(),
                  false
                )
              )
          )
          .withRegion(Regions.fromName(configuration.get().getAwsRegion().get()))
          .build();
    } else {
      elbClient = new AmazonElasticLoadBalancingClient();
    }

    if (configuration.isPresent() && configuration.get().getAwsEndpoint().isPresent()) {
      elbClient.setEndpoint(configuration.get().getAwsEndpoint().get());
    }

    return elbClient;
  }

  @Bean(name = GOOGLE_CLOUD_COMPUTE_SERVICE)
  public Optional<Compute> provideComputeService(BaragonConfiguration config)
    throws Exception {
    if (!config.getGoogleCloudConfiguration().isEnabled()) {
      return Optional.absent();
    }
    HttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();
    JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();

    GoogleCredential credential = null;

    if (config.getGoogleCloudConfiguration().getGoogleCredentialsFile() != null) {
      File credentialsFile = new File(
        config.getGoogleCloudConfiguration().getGoogleCredentialsFile()
      );
      credential = GoogleCredential.fromStream(new FileInputStream(credentialsFile));
    } else if (config.getGoogleCloudConfiguration().getGoogleCredentials() != null) {
      credential =
        GoogleCredential.fromStream(
          new ByteArrayInputStream(
            config.getGoogleCloudConfiguration().getGoogleCredentials().getBytes("UTF-8")
          )
        );
    } else {
      throw new RuntimeException(
        "Must specify googleCloudCredentials or googleCloudCredentialsFile when using google cloud api"
      );
    }

    if (credential.createScopedRequired()) {
      credential =
        credential.createScoped(
          Collections.singletonList("https://www.googleapis.com/auth/cloud-platform")
        );
    }

    return Optional.of(
      new Compute.Builder(httpTransport, jsonFactory, credential)
        .setApplicationName("BaragonService")
        .build()
    );
  }

  @Bean(name = BARAGON_AWS_ELB_CLIENT_V2)
  public com.amazonaws.services.elasticloadbalancingv2.AmazonElasticLoadBalancing providesAwsElbClientV2(
    Optional<ElbConfiguration> configuration
  ) {
    com.amazonaws.services.elasticloadbalancingv2.AmazonElasticLoadBalancing elbClient;
    if (
      configuration.isPresent() &&
      configuration.get().getAwsAccessKeyId() != null &&
      configuration.get().getAwsAccessKeySecret() != null &&
      configuration.get().getAwsRegion().isPresent()
    ) {
      elbClient =
        com
          .amazonaws.services.elasticloadbalancingv2.AmazonElasticLoadBalancingClientBuilder.standard()
          .withCredentials(
            new AWSStaticCredentialsProvider(
              new BasicAWSCredentials(
                configuration.get().getAwsAccessKeyId(),
                configuration.get().getAwsAccessKeySecret()
              )
            )
          )
          .withClientConfiguration(
            new ClientConfigurationFactory()
              .getConfig()
              .withRetryPolicy(
                new RetryPolicy(
                  RETRY_CONDITION,
                  new ExponentialBackoffStrategy(
                    configuration
                      .or(new ElbConfiguration())
                      .getAwsElbClientBackoffBaseDelayMilliseconds(),
                    configuration
                      .or(new ElbConfiguration())
                      .getAwsElbClientBackoffMaxBackoffMilliseconds()
                  ),
                  configuration.get().getAwsElbClientRetries(),
                  false
                )
              )
          )
          .withRegion(Regions.fromName(configuration.get().getAwsRegion().get()))
          .build();
    } else {
      elbClient =
        new com.amazonaws.services.elasticloadbalancingv2.AmazonElasticLoadBalancingClient();
    }

    if (configuration.isPresent() && configuration.get().getAwsEndpoint().isPresent()) {
      elbClient.setEndpoint(configuration.get().getAwsEndpoint().get());
    }

    return elbClient;
  }

  @Bean(destroyMethod = "")
  public CuratorFramework provideCurator(
    ZooKeeperConfiguration config,
    BaragonConnectionStateListener connectionStateListener
  ) {
    CuratorFramework client = CuratorFrameworkFactory
      .builder()
      .connectString(config.getQuorum())
      .sessionTimeoutMs(config.getSessionTimeoutMillis())
      .connectionTimeoutMs(config.getConnectTimeoutMillis())
      .retryPolicy(
        new ExponentialBackoffRetry(
          config.getRetryBaseSleepTimeMilliseconds(),
          config.getRetryMaxTries()
        )
      )
      .defaultData(new byte[0])
      .build();

    client.getConnectionStateListenable().addListener(connectionStateListener);

    client.start();

    return client.usingNamespace(config.getZkNamespace());
  }

  @Bean
  public Optional<SentryConfiguration> sentryConfiguration(
    final BaragonConfiguration config
  ) {
    return config.getSentryConfiguration();
  }

  @Bean
  public UpstreamResolver provideUpstreamResolver(BaragonConfiguration config) {
    return new UpstreamResolver(
      config.getMaxResolveCacheSize(),
      config.getExpireResolveCacheAfterDays()
    );
  }

  /** The metric registry the running service reports to. */
  @Bean
  public MetricRegistry provideRegistry(Environment environment) {
    return environment.metrics();
  }
}
