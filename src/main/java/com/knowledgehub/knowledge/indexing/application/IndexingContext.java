package com.knowledgehub.knowledge.indexing.application;

import com.knowledgehub.knowledge.domain.ChunkVector;
import com.knowledgehub.knowledge.indexing.domain.Chunk;
import com.knowledgehub.knowledge.indexing.domain.ChunkConfig;
import com.knowledgehub.knowledge.indexing.domain.CodeEntity;
import com.knowledgehub.knowledge.ingestion.domain.RawArtifact;
import java.util.List;

/**
 * Per-artifact accumulator carried through the indexing pipeline. Each stage reads what it needs
 * and writes its output back here; the context is the only place run state lives, so the stages
 * stay stateless and reusable (e.g. by sync). One context = one artifact.
 */
class IndexingContext {

  private final RawArtifact artifact;
  private final ChunkConfig config;

  private List<Chunk> chunks = List.of();
  private List<CodeEntity> entities = List.of();
  private List<Chunk> newChunks = List.of();
  private List<ChunkVector> vectors = List.of();
  private int cached;
  private int relationshipsLinked;
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

  void setChunked(List<Chunk> chunks, List<CodeEntity> entities) {
    this.chunks = List.copyOf(chunks);
    this.entities = List.copyOf(entities);
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
