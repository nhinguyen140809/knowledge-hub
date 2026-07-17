package com.knowledgehub.access.domain.port;

import com.knowledgehub.access.domain.Principal;
import com.knowledgehub.access.domain.Role;
import java.util.List;
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

  /** Adds {@code memberId} to {@code groupId} (a {@code MEMBER_OF} edge). */
  void addMember(String groupId, String memberId);

  /** Removes {@code memberId} from {@code groupId}. */
  void removeMember(String groupId, String memberId);

  /** The direct members of a group. */
  List<String> membersOf(String groupId);
}
