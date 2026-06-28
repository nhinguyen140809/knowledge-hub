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

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class QdrantVectorAdapterTests {

  @Autowired private QdrantVectorAdapter adapter;

  @BeforeEach
  void clean() {
    adapter.deleteByChunkIds(List.of("chunk-a", "chunk-b"));
  }

  @Test
  void upsertThenSearchReturnsTheNearestChunkFirst() {
    adapter.upsert(
        List.of(
            new ChunkVector("chunk-a", unit(0), Map.of("source_id", "src-1")),
            new ChunkVector("chunk-b", unit(1), Map.of("source_id", "src-1"))));

    List<ScoredId> hits = adapter.search(unit(0), 2, Filter.unrestricted());

    assertThat(hits).extracting(ScoredId::chunkId).containsExactly("chunk-a", "chunk-b");
  }

  @Test
  void aclPreFilterExcludesDisallowedSources() {
    adapter.upsert(List.of(new ChunkVector("chunk-a", unit(0), Map.of("source_id", "src-1"))));

    List<ScoredId> hits = adapter.search(unit(0), 5, Filter.ofSources(Set.of("src-2")));

    assertThat(hits).isEmpty();
  }

  @Test
  void emptyAllowListReadsNothing() {
    adapter.upsert(List.of(new ChunkVector("chunk-a", unit(0), Map.of("source_id", "src-1"))));

    List<ScoredId> hits = adapter.search(unit(0), 5, Filter.ofSources(Set.of()));

    assertThat(hits).isEmpty();
  }

  @Test
  void deleteByChunkIdsRemovesTheVector() {
    adapter.upsert(List.of(new ChunkVector("chunk-a", unit(0), Map.of("source_id", "src-1"))));
    assertThat(adapter.search(unit(0), 5, Filter.unrestricted())).isNotEmpty();

    adapter.deleteByChunkIds(List.of("chunk-a"));

    assertThat(adapter.search(unit(0), 5, Filter.unrestricted())).isEmpty();
  }

  /** A 1536-dim unit vector with 1.0 at the given index (matches the collection dimension). */
  private static float[] unit(int index) {
    float[] vector = new float[1536];
    vector[index] = 1f;
    return vector;
  }
}
