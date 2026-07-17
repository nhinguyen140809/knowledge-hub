package com.knowledgehub.knowledge.graph.application;

import com.knowledgehub.knowledge.analysis.domain.Chunk;
import com.knowledgehub.knowledge.analysis.domain.PendingReference;
import com.knowledgehub.knowledge.domain.Relationship;
import com.knowledgehub.knowledge.graph.domain.LinkCandidate;
import com.knowledgehub.knowledge.graph.domain.ResolutionScope;
import com.knowledgehub.knowledge.graph.domain.port.CrossArtifactLinker;
import com.knowledgehub.knowledge.graph.domain.port.EntityResolver;
import com.knowledgehub.knowledge.graph.domain.port.RelationshipRepository;
import com.knowledgehub.knowledge.ingestion.domain.RawArtifact;
import com.knowledgehub.shared.config.AppProperties;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Links one already-analyzed artifact into the knowledge graph. The analysis pass has already read
 * everything the syntax decides — this service only finishes the job: it resolves the pending
 * references against the stored entities (one batched lookup, source-preferred then any source),
 * runs every cross-artifact linker that applies (heuristic edges, kept only at or above the
 * configured confidence threshold), and upserts the result together with the artifact's
 * already-resolved local relations. Writes are idempotent, so a re-link never duplicates an edge
 * and this can run again on a sync delta.
 *
 * <p>This is the linking step the indexing pipeline calls after a file's nodes are stored — only
 * then do the reference targets exist. A pending reference that does not resolve is dropped rather
 * than guessed; re-linking unchanged content is how it eventually resolves once its target is
 * indexed.
 */
@Service
public class LinkingService {

  private static final Logger log = LoggerFactory.getLogger(LinkingService.class);

  private final EntityResolver resolver;
  private final List<CrossArtifactLinker> crossArtifactLinkers;
  private final RelationshipRepository relationships;
  private final double confidenceThreshold;

  public LinkingService(
      EntityResolver resolver,
      List<CrossArtifactLinker> crossArtifactLinkers,
      RelationshipRepository relationships,
      AppProperties properties) {
    this.resolver = resolver;
    this.crossArtifactLinkers = crossArtifactLinkers;
    this.relationships = relationships;
    this.confidenceThreshold = properties.linking().confidenceThreshold();
  }

  /**
   * Links the artifact, writing its local relations, resolved pending references, and accepted
   * cross-artifact relationships.
   *
   * @param artifact the artifact whose nodes are already stored
   * @param chunks the artifact's chunks (link sources for cross-artifact edges)
   * @param relations same-artifact edges the analysis pass already resolved
   * @param pendingReferences named references from analysis, resolved here against stored entities
   * @return how many relationships were written and how many candidates were dropped
   */
  public LinkSummary link(
      RawArtifact artifact,
      List<Chunk> chunks,
      List<Relationship> relations,
      List<PendingReference> pendingReferences) {
    List<Relationship> toWrite = new ArrayList<>(relations);
    ResolutionScope scope = new ResolutionScope(artifact.provenance().sourceId());
    toWrite.addAll(resolve(pendingReferences, scope));

    int dropped = 0;
    for (CrossArtifactLinker linker : crossArtifactLinkers) {
      if (!linker.supports(artifact)) {
        continue;
      }
      for (LinkCandidate candidate : linker.link(artifact, chunks)) {
        if (candidate.score() >= confidenceThreshold) {
          toWrite.add(candidate.toRelationship());
        } else {
          dropped++;
        }
      }
    }

    relationships.upsertAll(toWrite);
    if (!toWrite.isEmpty() || dropped > 0) {
      log.debug(
          "Linked {} relationships ({} candidates dropped below {}) for {}",
          toWrite.size(),
          dropped,
          confidenceThreshold,
          artifact.path());
    }
    return new LinkSummary(toWrite.size(), dropped);
  }

  /**
   * Resolves the pending references in one round-trip and returns the edges whose target names
   * settled on an indexed entity; the rest are dropped rather than guessed.
   */
  private List<Relationship> resolve(List<PendingReference> refs, ResolutionScope scope) {
    if (refs.isEmpty()) {
      return List.of();
    }
    Set<String> names = refs.stream().map(PendingReference::targetName).collect(Collectors.toSet());
    Map<String, String> resolved = resolver.resolve(names, scope);
    List<Relationship> out = new ArrayList<>();
    for (PendingReference ref : refs) {
      String toId = resolved.get(ref.targetName());
      if (toId != null) {
        out.add(Relationship.deterministic(ref.fromId(), toId, ref.relationType()));
      }
    }
    return out;
  }
}
