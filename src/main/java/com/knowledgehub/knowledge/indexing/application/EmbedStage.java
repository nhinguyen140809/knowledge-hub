package com.knowledgehub.knowledge.indexing.application;

import com.knowledgehub.knowledge.domain.ChunkVector;
import com.knowledgehub.knowledge.domain.EmbeddingPort;
import com.knowledgehub.knowledge.indexing.domain.Chunk;
import com.knowledgehub.shared.pipeline.Stage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Third filter: embeds the new/changed chunks in one batch call and pairs each embedding with the
 * metadata the vector store needs (source for the ACL pre-filter, plus path/type/ref/line range).
 */
@Component
class EmbedStage implements Stage<IndexingContext> {

  private final EmbeddingPort embedding;

  EmbedStage(EmbeddingPort embedding) {
    this.embedding = embedding;
  }

  @Override
  public IndexingContext apply(IndexingContext context) {
    if (context.isSkipped() || context.newChunks().isEmpty()) {
      return context;
    }
    List<Chunk> chunks = context.newChunks();
    List<String> texts = chunks.stream().map(Chunk::text).toList();
    List<float[]> embeddings = embedding.embedBatch(texts);

    List<ChunkVector> vectors = new ArrayList<>(chunks.size());
    for (int i = 0; i < chunks.size(); i++) {
      vectors.add(
          new ChunkVector(chunks.get(i).chunkId(), embeddings.get(i), metadata(chunks.get(i))));
    }
    context.setVectors(vectors);
    return context;
  }

  private static Map<String, Object> metadata(Chunk chunk) {
    Map<String, Object> metadata = new HashMap<>();
    metadata.put("source_id", chunk.sourceId());
    metadata.put("file_id", chunk.fileId());
    metadata.put("path", chunk.path());
    metadata.put("type", chunk.type().wireName());
    metadata.put("line_start", chunk.lineStart());
    metadata.put("line_end", chunk.lineEnd());
    chunk
        .provenance()
        .version()
        .ifPresent(
            v -> {
              metadata.put("ref", v.ref());
              metadata.put("commit_sha", v.commitSha());
            });
    return metadata;
  }
}
