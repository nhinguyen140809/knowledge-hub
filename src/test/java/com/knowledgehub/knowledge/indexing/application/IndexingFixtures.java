package com.knowledgehub.knowledge.indexing.application;

import com.knowledgehub.knowledge.indexing.domain.Chunk;
import com.knowledgehub.knowledge.indexing.domain.ChunkType;
import com.knowledgehub.knowledge.ingestion.domain.FsProvenance;
import com.knowledgehub.knowledge.ingestion.domain.RawArtifact;
import com.knowledgehub.knowledge.ingestion.infrastructure.MediaTypes;
import com.knowledgehub.shared.id.IdFactory;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

/** Shared builders for indexing application-layer tests. */
final class IndexingFixtures {

  static final String SOURCE = "src";
  static final String PATH = "doc.md";

  private IndexingFixtures() {}

  static RawArtifact markdownArtifact(String text) {
    return RawArtifact.raw(
            PATH,
            MediaTypes.MARKDOWN,
            text.getBytes(StandardCharsets.UTF_8),
            new FsProvenance(SOURCE, PATH, "file-hash", Instant.EPOCH))
        .withText(text);
  }

  static Chunk docChunk(String text) {
    String hash = "hash-" + text.hashCode();
    return new Chunk(
        IdFactory.chunkId(SOURCE, PATH, hash),
        SOURCE,
        IdFactory.fileId(SOURCE, PATH),
        PATH,
        ChunkType.DOC,
        text,
        hash,
        3,
        1,
        2,
        null,
        new FsProvenance(SOURCE, PATH, "file-hash", Instant.EPOCH));
  }
}
