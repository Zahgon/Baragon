package com.hubspot.baragon.data;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.inject.Named;
import com.hubspot.baragon.BaragonDataModule;
import java.util.concurrent.atomic.AtomicReference;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.state.ConnectionState;
import org.apache.curator.framework.state.ConnectionStateListener;

@Named
@Singleton
public class BaragonConnectionStateListener implements ConnectionStateListener {
  private final AtomicReference<ConnectionState> connectionState;

  @Inject
  public BaragonConnectionStateListener(
    @Named(
      BaragonDataModule.BARAGON_ZK_CONNECTION_STATE
    ) AtomicReference<ConnectionState> connectionState
  ) {
    this.connectionState = connectionState;
  }

  @Override
  public void stateChanged(CuratorFramework client, ConnectionState newState) {
    connectionState.set(newState);
  }
}
