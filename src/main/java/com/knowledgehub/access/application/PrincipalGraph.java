package com.knowledgehub.access.application;

import com.knowledgehub.access.domain.Principal;
import java.util.List;
import java.util.Map;

/**
 * Every principal plus the direct membership edges, resolved in one call. Lets the tree view draw
 * its first frame (and find root principals — those in no group) without walking {@code members}
 * once per group.
 *
 * @param principals every principal
 * @param membership direct member ids per group id; a group with no members has no entry
 */
public record PrincipalGraph(List<Principal> principals, Map<String, List<String>> membership) {}
