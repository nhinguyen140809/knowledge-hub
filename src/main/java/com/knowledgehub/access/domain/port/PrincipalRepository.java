package com.knowledgehub.access.domain.port;

import com.knowledgehub.access.domain.Principal;
import com.knowledgehub.access.domain.Role;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Stores principals (subjects and groups) and their group membership. */
public interface PrincipalRepository {

  /** Upserts a principal by id. */
  Principal save(Principal principal);

  Optional<Principal> findById(String principalId);

  List<Principal> findAll();

  /** Removes a principal and all its relationships (membership, grants, credentials). */
  void deleteById(String principalId);

  /** Whether any principal with the given role exists (used to decide bootstrap). */
  boolean existsByRole(Role role);

  /** How many principals currently have the given role (used to guard the last admin). */
  long countByRole(Role role);

  /** Adds {@code memberId} to {@code groupId} (a {@code MEMBER_OF} edge). */
  void addMember(String groupId, String memberId);

  /**
   * Whether adding {@code memberId} to {@code groupId} would close a membership cycle — true when
   * {@code groupId} is already (transitively, including itself) a member of {@code memberId}, so
   * the new edge would complete a loop.
   */
  boolean wouldCreateCycle(String groupId, String memberId);

  /** Removes {@code memberId} from {@code groupId}. */
  void removeMember(String groupId, String memberId);

  /** The direct members of a group. */
  List<String> membersOf(String groupId);

  /**
   * Direct member ids per group id, for every group at once — the single-query alternative to one
   * {@link #membersOf} call per group. A group with no members has no entry.
   */
  Map<String, List<String>> membershipGraph();

  /**
   * Every principal reachable from {@code principalId} by walking {@code MEMBER_OF} outward: the
   * principal itself, every group it belongs to (directly or transitively), and so on. The node
   * set for one principal's access-graph view.
   */
  List<Principal> ancestorsOf(String principalId);
}
