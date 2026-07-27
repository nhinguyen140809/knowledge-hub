package com.knowledgehub.system.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.knowledgehub.TestcontainersConfiguration;
import com.knowledgehub.knowledge.domain.ChunkVector;
import com.knowledgehub.knowledge.infrastructure.persistence.QdrantVectorAdapter;
import com.knowledgehub.system.domain.port.VectorStoreMetrics;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

/** Counts by delta: the Testcontainers Qdrant collection is shared across the whole test run. */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class QdrantVectorStoreMetricsAdapterTests {

  private static final String CHUNK_ID = "stats-it-chunk";

  @Autowired private VectorStoreMetrics metrics;
  @Autowired private QdrantVectorAdapter vectors;

  @AfterEach
  void tearDown() {
    vectors.deleteByChunkIds(List.of(CHUNK_ID));
  }

  @Test
  void pointCountReflectsUpsertedVectors() {
    long before = metrics.pointCount();

    vectors.upsert(List.of(new ChunkVector(CHUNK_ID, unit(0), Map.of("source_id", "stats-src"))));

    assertThat(metrics.pointCount()).isEqualTo(before + 1);
  }

  /** A 1536-dim unit vector with 1.0 at the given index (matches the collection dimension). */
  private static float[] unit(int index) {
    float[] vector = new float[1536];
    vector[index] = 1f;
    return vector;
  }
}
