package com.knowledgehub.access.domain;

import java.util.List;

/**
 * A read grant: the sources a principal is allowed to read. The only permission modelled is read;
 * authorization is scoped to read access at the source level.
 *
 * @param principalId the principal the grant belongs to
 * @param sourceIds the sources it may read
 */
public record Grant(String principalId, List<String> sourceIds) {

  public Grant {
    sourceIds = List.copyOf(sourceIds);
  }
}
