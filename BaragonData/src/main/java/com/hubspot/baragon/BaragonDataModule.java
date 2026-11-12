package com.hubspot.baragon;

/**
 * The names the components of the state layer are known by, wherever they are wired: the settings a
 * request to an agent is sent with, the values the workers and the API share, and the watches over the
 * state store.
 */
public final class BaragonDataModule {

  public static final String BARAGON_AGENT_REQUEST_URI_FORMAT =
    "baragon.agent.request.uri.format";

  public static final String BARAGON_AGENT_BATCH_REQUEST_URI_FORMAT =
    "baragon.agent.batch.request.uri.format";

  public static final String BARAGON_AGENT_MAX_ATTEMPTS = "baragon.agent.maxAttempts";

  public static final String BARAGON_AGENT_REQUEST_TIMEOUT_MS =
    "baragon.agent.requestTimeoutMs";

  public static final String BARAGON_SERVICE_WORKER_LAST_START =
    "baragon.service.worker.lastStartedAt";

  public static final String BARAGON_ELB_WORKER_LAST_START =
    "baragon.service.elb.lastStartedAt";

  public static final String BARAGON_AUTH_KEY_MAP = "baragon.auth.keyMap";

  public static final String BARAGON_AUTH_KEY = "baragon.auth.key";

  public static final String BARAGON_AUTH_PATH_CACHE = "baragon.auth.pathCache";

  public static final String BARAGON_ZK_CONNECTION_STATE = "baragon.zk.connectionState";

  public static final String BARAGON_SERVICE_LEADER_LATCH = "baragon.service.leaderLatch";

  private BaragonDataModule() {}
}
