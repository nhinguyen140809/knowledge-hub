package com.knowledgehub.knowledge.graph.domain.port;

import com.knowledgehub.knowledge.analysis.domain.Chunk;
import com.knowledgehub.knowledge.graph.domain.LinkCandidate;
import com.knowledgehub.knowledge.ingestion.domain.RawArtifact;
import java.util.List;

/**
 * Proposes heuristic relationships from an artifact's chunks to the code they refer to (a document
 * mentioning a class, a path reference). Each implementation looks for one kind of signal and
 * scores its confidence; several run together and the caller keeps only candidates above a
 * threshold. They never write — they only propose — so the accept/reject policy stays in one place.
 */
public interface CrossArtifactLinker {

  /** Whether this linker applies to the artifact (e.g. only documents, not source code). */
  boolean supports(RawArtifact artifact);

  /**
   * Proposes cross-artifact links from the artifact's chunks to resolved entities.
   *
   * @param artifact the artifact being linked
   * @param chunks the artifact's chunks (their ids are the link source ends)
   * @return scored candidates; empty when no signal is found
   */
  List<LinkCandidate> link(RawArtifact artifact, List<Chunk> chunks);
}
