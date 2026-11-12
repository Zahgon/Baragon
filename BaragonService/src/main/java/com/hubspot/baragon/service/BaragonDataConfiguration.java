package com.hubspot.baragon.service;

import com.google.common.base.Optional;
import com.hubspot.baragon.BaragonDataModule;
import com.hubspot.baragon.config.AuthConfiguration;
import com.hubspot.baragon.data.BaragonAuthDatastore;
import com.hubspot.baragon.models.BaragonAuthKey;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicReference;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.cache.PathChildrenCache;
import org.apache.curator.framework.state.ConnectionState;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * The parts of the state store the service reads and writes: the datastores and the caches over
 * them, and the values they share - the connection state the health check reports, the key map the
 * API is guarded by and the watch that keeps it current.
 */
@Configuration
@ComponentScan(
  basePackages = {
    "com.hubspot.baragon.cache",
    "com.hubspot.baragon.data",
    "com.hubspot.baragon.managers",
    "com.hubspot.baragon.migrations",
    "com.hubspot.baragon.utils"
  }
)
public class BaragonDataConfiguration {

  @Bean
  public Random providesRandom() {
    return new Random();
  }

  @Bean(name = BaragonDataModule.BARAGON_ZK_CONNECTION_STATE)
  public AtomicReference<ConnectionState> providesConnectionState() {
    return new AtomicReference<>();
  }

  @Bean(name = BaragonDataModule.BARAGON_AUTH_KEY_MAP)
  public AtomicReference<Map<String, BaragonAuthKey>> providesBaragonAuthKeyMap(
    BaragonAuthDatastore datastore
  ) {
    return new AtomicReference<>(datastore.getAuthKeyMap());
  }

  @Bean(name = BaragonDataModule.BARAGON_AUTH_PATH_CACHE, destroyMethod = "")
  public PathChildrenCache providesAuthPathChildrenCache(
    CuratorFramework curatorFramework
  ) {
    return new PathChildrenCache(
      curatorFramework,
      BaragonAuthDatastore.AUTH_KEYS_PATH,
      false
    );
  }

  @Bean(name = BaragonDataModule.BARAGON_AUTH_KEY)
  public Optional<String> providesBaragonAuthKey(AuthConfiguration authConfiguration) {
    return authConfiguration.getKey();
  }
}
