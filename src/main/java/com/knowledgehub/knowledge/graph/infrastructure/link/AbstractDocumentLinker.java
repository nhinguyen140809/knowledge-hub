package com.knowledgehub.knowledge.graph.infrastructure.link;

import com.knowledgehub.knowledge.graph.domain.CrossArtifactLinker;
import com.knowledgehub.knowledge.indexing.domain.Chunk;
import com.knowledgehub.knowledge.indexing.domain.ChunkType;
import com.knowledgehub.knowledge.ingestion.domain.RawArtifact;
import java.util.List;
import java.util.Locale;

/**
 * Shared base for cross-artifact linkers that read prose out of document chunks: it applies to any
 * artifact that carries text and is not Java source, and exposes the artifact's document chunks.
 * Each subclass looks for one kind of signal (a named identifier, a path reference) over those
 * chunks.
 */
abstract class AbstractDocumentLinker implements CrossArtifactLinker {

  @Override
  public boolean supports(RawArtifact artifact) {
    return artifact.text() != null && !artifact.path().toLowerCase(Locale.ROOT).endsWith(".java");
  }

  /** The artifact's document chunks - the only chunks a cross-artifact linker reads. */
  protected static List<Chunk> documentChunks(List<Chunk> chunks) {
    return chunks.stream().filter(chunk -> chunk.type() == ChunkType.DOC).toList();
  }
}
