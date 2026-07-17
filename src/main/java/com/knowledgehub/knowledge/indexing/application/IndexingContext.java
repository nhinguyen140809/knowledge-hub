package com.knowledgehub.knowledge.indexing.application;

import com.knowledgehub.knowledge.analysis.domain.Chunk;
import com.knowledgehub.knowledge.analysis.domain.ChunkConfig;
import com.knowledgehub.knowledge.analysis.domain.CodeEntity;
import com.knowledgehub.knowledge.analysis.domain.PendingReference;
import com.knowledgehub.knowledge.domain.ChunkVector;
import com.knowledgehub.knowledge.domain.Relationship;
import com.knowledgehub.knowledge.ingestion.domain.RawArtifact;
import java.util.List;

/**
 * Per-artifact accumulator carried through the indexing pipeline. Each stage reads what it needs
 * and writes its output back here; the context is the only place run state lives, so the stages
 * stay stateless and reusable (e.g. by sync). One context = one artifact.
 */
class IndexingContext {

  // Inputs, set once at construction: the artifact to index and the chunking tunables.
  private final RawArtifact artifact;
  private final ChunkConfig config;

  // Everything the single analysis pass produced: chunks, the code entities alongside them, the
  // same-artifact edges already resolved, and the named references awaiting resolution by linking
  // (set by AnalyzeStage).
  private List<Chunk> chunks = List.of();
  private List<CodeEntity> entities = List.of();
  private List<Relationship> relations = List.of();
  private List<PendingReference> pendingReferences = List.of();
  // The subset of chunks whose content is not already indexed — the ones worth embedding — and how
  // many were skipped as already present (set by DedupStage).
  private List<Chunk> newChunks = List.of();
  private int cached;
  // Embeddings for the new chunks, ready for the vector store (set by EmbedStage).
  private List<ChunkVector> vectors = List.of();
  // Count of graph relationships written for the artifact (set by LinkStage).
  private int relationshipsLinked;
  // Set by any stage to short-circuit the rest of the pipeline for this artifact.
  private boolean skipped;
  private String skipReason;

  IndexingContext(RawArtifact artifact, ChunkConfig config) {
    this.artifact = artifact;
    this.config = config;
  }

  RawArtifact artifact() {
    return artifact;
  }

  ChunkConfig config() {
    return config;
  }

  String sourceId() {
    return artifact.provenance().sourceId();
  }

  List<Chunk> chunks() {
    return chunks;
  }

  List<CodeEntity> entities() {
    return entities;
  }

  List<Relationship> relations() {
    return relations;
  }

  List<PendingReference> pendingReferences() {
    return pendingReferences;
  }

  List<Chunk> newChunks() {
    return newChunks;
  }

  List<ChunkVector> vectors() {
    return vectors;
  }

  int cached() {
    return cached;
  }

  int relationshipsLinked() {
    return relationshipsLinked;
  }

  boolean isSkipped() {
    return skipped;
  }

  String skipReason() {
    return skipReason;
  }

  void setAnalyzed(
      List<Chunk> chunks,
      List<CodeEntity> entities,
      List<Relationship> relations,
      List<PendingReference> pendingReferences) {
    this.chunks = List.copyOf(chunks);
    this.entities = List.copyOf(entities);
    this.relations = List.copyOf(relations);
    this.pendingReferences = List.copyOf(pendingReferences);
  }

  void setNewChunks(List<Chunk> newChunks, int cached) {
    this.newChunks = List.copyOf(newChunks);
    this.cached = cached;
  }

  void setVectors(List<ChunkVector> vectors) {
    this.vectors = List.copyOf(vectors);
  }

  void setRelationshipsLinked(int relationshipsLinked) {
    this.relationshipsLinked = relationshipsLinked;
  }

  void markSkipped(String reason) {
    this.skipped = true;
    this.skipReason = reason;
  }
}
