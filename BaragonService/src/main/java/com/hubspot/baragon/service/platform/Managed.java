package com.hubspot.baragon.service.platform;

/** A component that is started with the service and stopped with it. */
public interface Managed {
  void start() throws Exception;

  void stop() throws Exception;
}
