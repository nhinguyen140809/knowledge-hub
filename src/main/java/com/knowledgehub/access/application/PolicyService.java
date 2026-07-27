package com.knowledgehub.access.application;

import com.knowledgehub.access.domain.DefaultPolicy;
import com.knowledgehub.access.domain.port.SystemConfigRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Administers the system-wide default read policy. */
@Service
public class PolicyService {

  private final SystemConfigRepository systemConfig;

  public PolicyService(SystemConfigRepository systemConfig) {
    this.systemConfig = systemConfig;
  }

  @Transactional(readOnly = true)
  public DefaultPolicy defaultPolicy() {
    return systemConfig.defaultPolicy();
  }

  @Transactional
  public void setDefaultPolicy(DefaultPolicy policy) {
    systemConfig.setDefaultPolicy(policy);
  }
}
