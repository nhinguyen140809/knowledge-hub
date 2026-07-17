package com.knowledgehub.knowledge.graph.application;

import com.knowledgehub.knowledge.analysis.domain.Chunk;
import com.knowledgehub.knowledge.graph.domain.CrossArtifactLinker;
import com.knowledgehub.knowledge.graph.domain.LinkCandidate;
import com.knowledgehub.knowledge.graph.domain.Relationship;
import com.knowledgehub.knowledge.graph.domain.RelationshipRepository;
import com.knowledgehub.knowledge.graph.domain.StructuralExtractor;
import com.knowledgehub.knowledge.ingestion.domain.RawArtifact;
import com.knowledgehub.shared.config.AppProperties;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Links one already-indexed artifact into the knowledge graph: it runs the structural extractor for
 * the artifact's language (deterministic edges) and every cross-artifact linker that applies
 * (heuristic edges), keeps only the candidates at or above the configured confidence threshold, and
 * upserts the result. Writes are idempotent, so a re-link never duplicates an edge and this can run
 * again on a sync delta.
 *
 * <p>This is the linking step the indexing pipeline calls after a file's nodes are stored. It works
 * on the artifact plus its chunks, never on the indexing pipeline context, so it carries no
 * dependency back into the indexing application layer.
 */
@Service
public class LinkingService {

  private static final Logger log = LoggerFactory.getLogger(LinkingService.class);

  private final List<StructuralExtractor> structuralExtractors;
  private final List<CrossArtifactLinker> crossArtifactLinkers;
  private final RelationshipRepository relationships;
  private final double confidenceThreshold;

  public LinkingService(
      List<StructuralExtractor> structuralExtractors,
      List<CrossArtifactLinker> crossArtifactLinkers,
      RelationshipRepository relationships,
      AppProperties properties) {
    this.structuralExtractors = structuralExtractors;
    this.crossArtifactLinkers = crossArtifactLinkers;
    this.relationships = relationships;
    this.confidenceThreshold = properties.linking().confidenceThreshold();
  }

  /**
   * Links the artifact, writing its structural and accepted cross-artifact relationships.
   *
   * @param artifact the artifact whose nodes are already stored
   * @param chunks the artifact's chunks (link sources for cross-artifact edges)
   * @return how many relationships were written and how many candidates were dropped
   */
  public LinkSummary link(RawArtifact artifact, List<Chunk> chunks) {
    List<Relationship> toWrite = new ArrayList<>();

    structuralExtractors.stream()
        .filter(extractor -> extractor.supports(artifact))
        .findFirst()
        .ifPresent(extractor -> toWrite.addAll(extractor.extract(artifact)));

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
}
