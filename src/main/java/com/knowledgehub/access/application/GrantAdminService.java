package com.knowledgehub.access.application;

import com.knowledgehub.access.domain.Principal;
import com.knowledgehub.access.domain.exception.AdminGrantException;
import com.knowledgehub.access.domain.port.GrantRepository;
import com.knowledgehub.access.domain.port.PrincipalRepository;
import java.util.Collection;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Administers direct read grants from a principal to sources. */
@Service
public class GrantAdminService {

  private final PrincipalRepository principals;
  private final GrantRepository grants;

  public GrantAdminService(PrincipalRepository principals, GrantRepository grants) {
    this.principals = principals;
    this.grants = grants;
  }

  @Transactional
  public void grant(String principalId, Collection<String> sourceIds) {
    Principal principal = get(principalId);
    if (principal.isAdmin()) {
      throw new AdminGrantException(principalId);
    }
    grants.grant(principalId, sourceIds);
  }

  @Transactional
  public void revokeGrant(String principalId, Collection<String> sourceIds) {
    get(principalId);
    grants.revoke(principalId, sourceIds);
  }

  private Principal get(String principalId) {
    return principals
        .findById(principalId)
        .orElseThrow(() -> new PrincipalNotFoundException(principalId));
  }
}
