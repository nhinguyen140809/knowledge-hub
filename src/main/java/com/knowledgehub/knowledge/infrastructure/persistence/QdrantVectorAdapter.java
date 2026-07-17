package com.knowledgehub.knowledge.infrastructure.persistence;

import static io.qdrant.client.ConditionFactory.matchKeyword;
import static io.qdrant.client.ConditionFactory.matchKeywords;
import static io.qdrant.client.PointIdFactory.id;
import static io.qdrant.client.VectorsFactory.vectors;
import static io.qdrant.client.WithPayloadSelectorFactory.include;

import com.knowledgehub.knowledge.domain.ChunkVector;
import com.knowledgehub.knowledge.domain.Filter;
import com.knowledgehub.knowledge.domain.ScoredId;
import com.knowledgehub.knowledge.domain.SparseVector;
import com.knowledgehub.knowledge.domain.port.HybridVectorStore;
import io.qdrant.client.QdrantClient;
import io.qdrant.client.ValueFactory;
import io.qdrant.client.grpc.JsonWithInt.Value;
import io.qdrant.client.grpc.Points.Condition;
import io.qdrant.client.grpc.Points.PointId;
import io.qdrant.client.grpc.Points.PointStruct;
import io.qdrant.client.grpc.Points.ScoredPoint;
import io.qdrant.client.grpc.Points.SearchPoints;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import javax.annotation.Nonnull;
import org.springframework.stereotype.Component;

/**
 * {@link com.knowledgehub.knowledge.domain.VectorStorePort} backed by Qdrant — the always-on vector
 * engine (the graph stays in Neo4j, linked by {@code chunk_id}). Implements {@link
 * HybridVectorStore} so retrieval can opt into Qdrant's native dense+sparse hybrid where it pays
 * off.
 *
 * <p>Qdrant point ids must be a uint64 or a UUID, but our {@code chunk_id} is a content-derived hex
 * string, so each point's id is a deterministic UUID derived from {@code chunk_id} and the original
 * {@code chunk_id} is kept in the payload (also used as the value returned by searches and to
 * delete by id). The ACL filter is pushed into the query as a hard {@code must} pre-filter so
 * disallowed sources are never returned.
 */
@Component
public class QdrantVectorAdapter implements HybridVectorStore {

  private static final String CHUNK_ID = "chunk_id";
  private static final String SOURCE_ID = "source_id";

  private final QdrantClient client;
  private final String collectionName;

  public QdrantVectorAdapter(
      QdrantClient client,
      @org.springframework.beans.factory.annotation.Value(
              "${spring.ai.vectorstore.qdrant.collection-name:knowledge-embeddings}")
          String collectionName) {
    this.client = client;
    this.collectionName = collectionName;
  }

  @Override
  public void upsert(List<ChunkVector> chunks) {
    if (chunks.isEmpty()) {
      return;
    }
    List<PointStruct> points = new ArrayList<>(chunks.size());
    for (ChunkVector chunk : chunks) {
      points.add(
          PointStruct.newBuilder()
              .setId(id(pointId(chunk.chunkId())))
              .setVectors(vectors(chunk.embedding()))
              .putAllPayload(payload(chunk))
              .build());
    }
    await(client.upsertAsync(collectionName, points));
  }

  @Override
  public List<ScoredId> search(float[] query, int k, Filter filter) {
    // Empty allow-list means nothing is readable — skip the round-trip.
    if (!filter.isUnrestricted() && filter.allowedSources().isEmpty()) {
      return List.of();
    }
    List<Float> vector = new ArrayList<>(query.length);
    for (float value : query) {
      vector.add(value);
    }
    SearchPoints.Builder request =
        SearchPoints.newBuilder()
            .setCollectionName(collectionName)
            .addAllVector(vector)
            .setLimit(k)
            .setWithPayload(include(List.of(CHUNK_ID)));
    aclFilter(filter).ifPresent(request::setFilter);

    List<ScoredPoint> hits = await(client.searchAsync(request.build()));
    List<ScoredId> results = new ArrayList<>(hits.size());
    for (ScoredPoint hit : hits) {
      results.add(new ScoredId(hit.getPayloadMap().get(CHUNK_ID).getStringValue(), hit.getScore()));
    }
    return results;
  }

  @Override
  public List<ScoredId> hybridSearch(float[] dense, SparseVector sparse, int k, Filter filter) {
    // No sparse vector is populated here, so there is nothing to fuse — fall back to dense search.
    // Native dense+sparse fusion via Qdrant's Query API can replace this once sparse vectors exist.
    return search(dense, k, filter);
  }

  @Override
  public void deleteByChunkIds(List<String> chunkIds) {
    if (chunkIds.isEmpty()) {
      return;
    }
    List<PointId> ids = new ArrayList<>(chunkIds.size());
    for (String chunkId : chunkIds) {
      ids.add(id(pointId(chunkId)));
    }
    await(client.deleteAsync(collectionName, ids));
  }

  @Override
  public void deleteBySource(String sourceId) {
    await(
        client.deleteAsync(
            collectionName,
            io.qdrant.client.grpc.Points.Filter.newBuilder()
                .addMust(matchKeyword(SOURCE_ID, sourceId))
                .build()));
  }

  /** Deterministic UUID for a chunk id, so re-upserting the same chunk overwrites its point. */
  @Nonnull
  @SuppressWarnings("null") // UUID.nameUUIDFromBytes never returns null
  private static UUID pointId(String chunkId) {
    return UUID.nameUUIDFromBytes(chunkId.getBytes(StandardCharsets.UTF_8));
  }

  private static Map<String, Value> payload(ChunkVector chunk) {
    Map<String, Value> payload = new HashMap<>();
    payload.put(CHUNK_ID, ValueFactory.value(chunk.chunkId()));
    chunk.metadata().forEach((key, value) -> payload.put(key, toValue(value)));
    return payload;
  }

  private static Value toValue(Object value) {
    return switch (value) {
      case null -> ValueFactory.nullValue();
      case String s -> ValueFactory.value(s);
      case Boolean b -> ValueFactory.value(b);
      case Integer i -> ValueFactory.value(i.longValue());
      case Long l -> ValueFactory.value(l);
      case Double d -> ValueFactory.value(d);
      case Float f -> ValueFactory.value(f.doubleValue());
      default -> ValueFactory.value(value.toString());
    };
  }

  private static Optional<io.qdrant.client.grpc.Points.Filter> aclFilter(Filter filter) {
    if (filter.isUnrestricted()) {
      return Optional.empty();
    }
    Condition condition = matchKeywords(SOURCE_ID, List.copyOf(filter.allowedSources()));
    return Optional.of(io.qdrant.client.grpc.Points.Filter.newBuilder().addMust(condition).build());
  }

  private static <T> T await(Future<T> future) {
    try {
      return future.get();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException("Interrupted during a Qdrant operation", e);
    } catch (ExecutionException e) {
      throw new IllegalStateException("Qdrant operation failed", e);
    }
  }
}
