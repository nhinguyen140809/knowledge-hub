package com.knowledgehub.access.domain.port;

import com.knowledgehub.access.domain.DefaultPolicy;

/** Reads and writes system-wide access settings (a config singleton). */
public interface SystemConfigRepository {

  /** The configured default policy, or {@link DefaultPolicy#DENY} if none has been set. */
  DefaultPolicy defaultPolicy();

  /** Sets the default policy. */
  void setDefaultPolicy(DefaultPolicy policy);
}
