package com.knowledgehub.knowledge.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.knowledgehub.TestcontainersConfiguration;
import com.knowledgehub.knowledge.domain.ChunkVector;
import com.knowledgehub.knowledge.domain.Filter;
import com.knowledgehub.knowledge.domain.ScoredId;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.neo4j.core.Neo4jClient;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class Neo4jVectorAdapterTests {

  @Autowired private Neo4jVectorAdapter adapter;
  @Autowired private Neo4jClient neo4jClient;

  @BeforeEach
  void clean() {
    neo4jClient.query("MATCH (c:Chunk) DETACH DELETE c").run();
  }

  @Test
  void upsertThenSearchReturnsTheNearestChunkFirst() throws InterruptedException {
    adapter.upsert(
        List.of(
            new ChunkVector("chunk-a", unit(0), Map.of("source_id", "src-1")),
            new ChunkVector("chunk-b", unit(1), Map.of("source_id", "src-1"))));

    List<ScoredId> hits = searchUntilFound(unit(0), 2, Filter.unrestricted());

    assertThat(hits.get(0).chunkId()).isEqualTo("chunk-a");
  }

  @Test
  void aclPreFilterExcludesDisallowedSources() throws InterruptedException {
    adapter.upsert(List.of(new ChunkVector("chunk-a", unit(0), Map.of("source_id", "src-1"))));
    searchUntilFound(unit(0), 5, Filter.unrestricted()); // ensure indexed

    List<ScoredId> hits = adapter.search(unit(0), 5, Filter.ofSources(Set.of("src-2")));

    assertThat(hits).isEmpty();
  }

  @Test
  void deleteByChunkIdsRemovesTheVector() throws InterruptedException {
    adapter.upsert(List.of(new ChunkVector("chunk-a", unit(0), Map.of("source_id", "src-1"))));
    searchUntilFound(unit(0), 5, Filter.unrestricted());

    adapter.deleteByChunkIds(List.of("chunk-a"));

    assertThat(adapter.search(unit(0), 5, Filter.unrestricted())).isEmpty();
  }

  /** A 1536-dim unit vector with 1.0 at the given index (matches the index dimension). */
  private static float[] unit(int index) {
    float[] vector = new float[1536];
    vector[index] = 1f;
    return vector;
  }

  /** The vector index may lag the write briefly; poll until results appear. */
  private List<ScoredId> searchUntilFound(float[] query, int k, Filter filter)
      throws InterruptedException {
    for (int attempt = 0; attempt < 25; attempt++) {
      List<ScoredId> hits = adapter.search(query, k, filter);
      if (!hits.isEmpty()) {
        return hits;
      }
      Thread.sleep(200);
    }
    return adapter.search(query, k, filter);
  }
}
