package com.knowledgehub.access.domain.port;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Stores read grants and answers the authorization queries built on them. */
public interface GrantRepository {

  /** Grants a principal read access to the given sources (idempotent). */
  void grant(String principalId, Collection<String> sourceIds);

  /** Revokes a principal's read access to the given sources. */
  void revoke(String principalId, Collection<String> sourceIds);

  /** The sources a principal lists as directly granted. */
  List<String> grantedSources(String principalId);

  /**
   * The sources readable through a principal's own grants and those of every group it belongs to,
   * resolved recursively. This is the union at the heart of authorization.
   */
  Set<String> readableSourcesFor(String principalId);

  /**
   * For a principal, which granting principals (itself or a group it belongs to, recursively) make
   * each readable source readable — used to explain effective permissions.
   */
  Map<String, Set<String>> grantingPrincipalsFor(String principalId);

  /** Every source that appears in some grant; under an allow policy these become restricted. */
  Set<String> allGrantedSources();
}
