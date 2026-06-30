package com.knowledgehub.access.application;

import com.knowledgehub.access.domain.DefaultPolicy;
import java.util.Map;
import java.util.Set;

/**
 * A principal's resolved read access, for inspection and debugging. {@code readableSources} is the
 * exact set the retrieval pre-filter uses; {@code grantedVia} explains, for each explicitly granted
 * source, which principals (the principal itself or a group it belongs to) grant it, so one can see
 * why a source is or is not readable.
 *
 * @param principalId the principal inspected
 * @param defaultPolicy the policy in force
 * @param readableSources the sources the principal may actually read
 * @param grantedVia for each granted source, the granting principals (self or group)
 */
public record EffectivePermissions(
    String principalId,
    DefaultPolicy defaultPolicy,
    Set<String> readableSources,
    Map<String, Set<String>> grantedVia) {}
