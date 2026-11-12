package com.hubspot.baragon.agent;

import com.google.common.base.Optional;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Scopes;
import com.google.inject.Singleton;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.name.Named;
import com.hubspot.baragon.BaragonDataModule;
import com.hubspot.baragon.cache.BaragonStateCache;
import com.hubspot.baragon.config.AuthConfiguration;
import com.hubspot.baragon.data.BaragonAgentResponseDatastore;
import com.hubspot.baragon.data.BaragonAliasDatastore;
import com.hubspot.baragon.data.BaragonAuthDatastore;
import com.hubspot.baragon.data.BaragonConnectionStateListener;
import com.hubspot.baragon.data.BaragonKnownAgentsDatastore;
import com.hubspot.baragon.data.BaragonLoadBalancerDatastore;
import com.hubspot.baragon.data.BaragonRequestDatastore;
import com.hubspot.baragon.data.BaragonResponseHistoryDatastore;
import com.hubspot.baragon.data.BaragonStateDatastore;
import com.hubspot.baragon.data.BaragonWorkerDatastore;
import com.hubspot.baragon.data.BaragonZkMetaDatastore;
import com.hubspot.baragon.managers.BaragonAuthManager;
import com.hubspot.baragon.migrations.ServiceDomainsMigration;
import com.hubspot.baragon.migrations.UpstreamsMigration;
import com.hubspot.baragon.migrations.ZkDataMigration;
import com.hubspot.baragon.migrations.ZkDataMigrationRunner;
import com.hubspot.baragon.models.BaragonAuthKey;
import com.hubspot.baragon.utils.ZkParallelFetcher;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicReference;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.cache.PathChildrenCache;
import org.apache.curator.framework.state.ConnectionState;

/**
 * The parts of the state store an agent reads and writes, wired the way the agent wires everything
 * else: the datastores and the caches over them, and the values they share.
 */
public class BaragonAgentDataModule extends AbstractModule {

  @Override
  protected void configure() {
    bind(BaragonStateCache.class).in(Scopes.SINGLETON);

    // Datastores
    bind(BaragonAuthDatastore.class).in(Scopes.SINGLETON);
    bind(BaragonKnownAgentsDatastore.class).in(Scopes.SINGLETON);
    bind(BaragonLoadBalancerDatastore.class).in(Scopes.SINGLETON);
    bind(BaragonStateDatastore.class).in(Scopes.SINGLETON);
    bind(BaragonWorkerDatastore.class).in(Scopes.SINGLETON);
    bind(BaragonAgentResponseDatastore.class).in(Scopes.SINGLETON);
    bind(BaragonRequestDatastore.class).in(Scopes.SINGLETON);
    bind(BaragonResponseHistoryDatastore.class).in(Scopes.SINGLETON);
    bind(BaragonZkMetaDatastore.class).in(Scopes.SINGLETON);
    bind(BaragonAliasDatastore.class).in(Scopes.SINGLETON);

    bind(ZkParallelFetcher.class).in(Scopes.SINGLETON);

    bind(BaragonConnectionStateListener.class).in(Scopes.SINGLETON);
    bind(ZkDataMigrationRunner.class).in(Scopes.SINGLETON);

    bind(BaragonAuthManager.class).in(Scopes.SINGLETON);

    Multibinder<ZkDataMigration> zkMigrationBinder = Multibinder.newSetBinder(
      binder(),
      ZkDataMigration.class
    );
    zkMigrationBinder.addBinding().to(UpstreamsMigration.class);
    zkMigrationBinder.addBinding().to(ServiceDomainsMigration.class);
  }

  @Provides
  @Singleton
  public Random providesRandom() {
    return new Random();
  }

  @Provides
  @Singleton
  @Named(BaragonDataModule.BARAGON_ZK_CONNECTION_STATE)
  public AtomicReference<ConnectionState> providesConnectionState() {
    return new AtomicReference<>();
  }

  @Provides
  @Singleton
  @Named(BaragonDataModule.BARAGON_AUTH_KEY_MAP)
  public AtomicReference<Map<String, BaragonAuthKey>> providesBaragonAuthKeyMap(
    BaragonAuthDatastore datastore
  ) {
    return new AtomicReference<>(datastore.getAuthKeyMap());
  }

  @Provides
  @Singleton
  @Named(BaragonDataModule.BARAGON_AUTH_PATH_CACHE)
  public PathChildrenCache providesAuthPathChildrenCache(
    CuratorFramework curatorFramework
  ) {
    return new PathChildrenCache(
      curatorFramework,
      BaragonAuthDatastore.AUTH_KEYS_PATH,
      false
    );
  }

  @Provides
  @Named(BaragonDataModule.BARAGON_AUTH_KEY)
  public Optional<String> providesBaragonAuthKey(AuthConfiguration authConfiguration) {
    return authConfiguration.getKey();
  }
}
