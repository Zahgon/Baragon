package com.hubspot.baragon.service.platform;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** The components the service starts and stops, in the order they were added. */
public class LifecycleEnvironment {
  private static final Logger LOG = LoggerFactory.getLogger(LifecycleEnvironment.class);

  private final List<Managed> managed = Collections.synchronizedList(new ArrayList<>());

  public void manage(Managed component) {
    managed.add(component);
  }

  public void start() throws Exception {
    synchronized (managed) {
      for (Managed component : managed) {
        component.start();
      }
    }
  }

  public void stop() {
    synchronized (managed) {
      for (int i = managed.size() - 1; i >= 0; i--) {
        try {
          managed.get(i).stop();
        } catch (Exception e) {
          LOG.warn("Unable to stop {}", managed.get(i), e);
        }
      }
    }
  }
}
