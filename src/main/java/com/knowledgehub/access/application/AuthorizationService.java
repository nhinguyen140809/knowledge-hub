package com.knowledgehub.access.application;

import com.knowledgehub.access.domain.AuthenticatedPrincipal;
import com.knowledgehub.access.domain.DefaultPolicy;
import com.knowledgehub.access.domain.port.Authorizer;
import com.knowledgehub.access.domain.port.GrantRepository;
import com.knowledgehub.access.domain.port.SystemConfigRepository;
import com.knowledgehub.knowledge.ingestion.domain.Source;
import com.knowledgehub.knowledge.ingestion.domain.port.SourceRepository;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

/**
 * Resolves what a principal may read. Effective permissions are the union of the principal's own
 * grants and those of every group it belongs to (resolved recursively in one traversal). The
 * default policy then decides sources with no grant: under deny only granted sources are readable;
 * under allow every source is readable except those that appear in some grant (which become
 * restricted to the principals granted them).
 */
@Service
public class AuthorizationService implements Authorizer {

  private final GrantRepository grants;
  private final SystemConfigRepository systemConfig;
  private final SourceRepository sources;

  public AuthorizationService(
      GrantRepository grants, SystemConfigRepository systemConfig, SourceRepository sources) {
    this.grants = grants;
    this.systemConfig = systemConfig;
    this.sources = sources;
  }

  @Override
  public Set<String> readableSources(AuthenticatedPrincipal principal) {
    if (principal.isAdmin()) {
      return sources.findAll().stream()
          .map(Source::sourceId)
          .collect(Collectors.toCollection(LinkedHashSet::new));
    }
    Set<String> granted = grants.readableSourcesFor(principal.principalId());
    if (systemConfig.defaultPolicy() == DefaultPolicy.DENY) {
      return granted;
    }
    Set<String> readable =
        sources.findAll().stream()
            .map(Source::sourceId)
            .collect(Collectors.toCollection(LinkedHashSet::new));
    readable.removeAll(grants.allGrantedSources());
    readable.addAll(granted);
    return readable;
  }

  @Override
  public boolean isAdmin(AuthenticatedPrincipal principal) {
    return principal != null && principal.isAdmin();
  }
}
