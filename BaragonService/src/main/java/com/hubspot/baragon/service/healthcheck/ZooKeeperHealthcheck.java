package com.hubspot.baragon.service.healthcheck;

import com.codahale.metrics.health.HealthCheck;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.inject.Named;
import com.hubspot.baragon.BaragonDataModule;
import java.util.concurrent.atomic.AtomicReference;
import org.apache.curator.framework.state.ConnectionState;

@Named
@Singleton
public class ZooKeeperHealthcheck extends HealthCheck {
  private final AtomicReference<ConnectionState> connectionState;

  @Inject
  public ZooKeeperHealthcheck(
    @Named(
      BaragonDataModule.BARAGON_ZK_CONNECTION_STATE
    ) AtomicReference<ConnectionState> connectionState
  ) {
    this.connectionState = connectionState;
  }

  @Override
  protected Result check() throws Exception {
    final ConnectionState currentConnectionState = connectionState.get();

    if (currentConnectionState == null) {
      return Result.unhealthy("Connection state is null");
    }

    switch (currentConnectionState) {
      case CONNECTED:
      case RECONNECTED:
        return Result.healthy(currentConnectionState.name());
      default:
        return Result.unhealthy(currentConnectionState.name());
    }
  }
}
