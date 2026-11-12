package com.hubspot.baragon.managers;

import com.google.common.base.Optional;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.inject.Named;
import com.hubspot.baragon.BaragonDataModule;
import com.hubspot.baragon.models.BaragonAuthKey;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

@Named
@Singleton
public class BaragonAuthManager {
  private final AtomicReference<Map<String, BaragonAuthKey>> authKeys;

  @Inject
  public BaragonAuthManager(
    @Named(
      BaragonDataModule.BARAGON_AUTH_KEY_MAP
    ) AtomicReference<Map<String, BaragonAuthKey>> authKeys
  ) {
    this.authKeys = authKeys;
  }

  public boolean isAuthenticated(String authKey) {
    final Optional<BaragonAuthKey> maybeAuthKey = Optional.fromNullable(
      authKeys.get().get(authKey)
    );

    return (
      maybeAuthKey.isPresent() &&
      (
        !maybeAuthKey.get().getExpiredAt().isPresent() ||
        maybeAuthKey.get().getExpiredAt().get() > System.currentTimeMillis()
      )
    );
  }
}
