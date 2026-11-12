package com.hubspot.baragon.service;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import com.codahale.metrics.MetricRegistry;
import com.hubspot.baragon.config.ZooKeeperConfiguration;
import com.hubspot.baragon.service.config.BaragonConfiguration;
import com.hubspot.baragon.service.managers.AgentManager;
import com.hubspot.baragon.service.managers.TestAgentManager;
import com.hubspot.baragon.service.platform.Environment;
import org.apache.curator.test.TestingServer;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;

/**
 * The components of the service, built as the service builds them, around a state store of this
 * test's own: a ZooKeeper server in this process, under a namespace of its own, and an agent manager
 * that records what it was asked to send instead of sending it.
 */
@Configuration
@Import({ ObjectMapperConfiguration.class, BaragonServiceConfiguration.class })
public class BaragonServiceTestModule {
  private static final String TEST_NAMESPACE = "baragontesting";

  private final TestingServer ts;

  public BaragonServiceTestModule() throws Exception {
    this.ts = new TestingServer();

    LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
    Logger rootLogger = context.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
    rootLogger.setLevel(
      Level.toLevel(System.getProperty("baragon.test.log.level", "WARN"))
    );

    Logger hsLogger = context.getLogger("com.hubspot");
    hsLogger.setLevel(
      Level.toLevel(System.getProperty("baragon.test.log.level.for.com.hubspot", "INFO"))
    );
  }

  @Bean
  public TestingServer testingServer() {
    return ts;
  }

  @Bean
  public BaragonConfiguration baragonConfiguration() {
    return getBaragonConfigurationForTestingServer(ts);
  }

  @Bean
  public Environment baragonEnvironment() {
    return new Environment("test-env", ObjectMappers.forApi(), null, new MetricRegistry());
  }

  /**
   * The agent manager a test is given, in place of the one that talks to agents. The method is not named
   * after the bean it returns: a @Bean method whose name is already taken by a registered bean definition is
   * skipped, and this one would then never mark anything primary, leaving the manager that talks to agents as
   * the one the workers are given.
   */
  @Bean
  @Primary
  public AgentManager recordingAgentManager(TestAgentManager testAgentManager) {
    return testAgentManager;
  }

  /** The components of the service, built and ready to be read by a test. */
  public AnnotationConfigApplicationContext getContext() {
    final AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
    context.register(BaragonServiceTestModule.class, TestAgentManager.class);
    context.getBeanFactory().registerSingleton("baragonServiceTestModule", this);
    context.refresh();
    return context;
  }

  private static BaragonConfiguration getBaragonConfigurationForTestingServer(
    final TestingServer ts
  ) {
    BaragonConfiguration config = new BaragonConfiguration();
    ZooKeeperConfiguration zookeeperConfiguration = new ZooKeeperConfiguration();
    zookeeperConfiguration.setQuorum(ts.getConnectString());
    zookeeperConfiguration.setConnectTimeoutMillis(1000);
    zookeeperConfiguration.setRetryBaseSleepTimeMilliseconds(1000);
    zookeeperConfiguration.setRetryMaxTries(1);
    zookeeperConfiguration.setSessionTimeoutMillis(1000);
    zookeeperConfiguration.setZkNamespace(TEST_NAMESPACE);
    config.setZooKeeperConfiguration(zookeeperConfiguration);
    // the hostname the service knows itself by, which a test does not read from its own networking
    config.setHostname("localhost");

    return config;
  }
}
