package com.knowledgehub.knowledge.indexing.application;

import com.knowledgehub.knowledge.domain.ChunkVector;
import com.knowledgehub.knowledge.domain.EmbeddingPort;
import com.knowledgehub.knowledge.domain.VectorStorePort;
import com.knowledgehub.knowledge.indexing.domain.CommitRepository;
import com.knowledgehub.knowledge.ingestion.domain.CommitHistoryPort;
import com.knowledgehub.knowledge.ingestion.domain.CommitRecord;
import com.knowledgehub.knowledge.ingestion.domain.Source;
import com.knowledgehub.shared.config.AppProperties;
import com.knowledgehub.shared.id.IdFactory;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Indexes a source's commit history as knowledge: each new commit becomes a searchable {@code
 * :Commit} node (its message embedded for semantic search) linked to the files it modified. History
 * is append-only, so the work per run is only what is new — the walk stops at the last known
 * commit, already-stored hashes are filtered out, and only fresh messages are embedded.
 *
 * <p>The message vectors are written before the graph nodes: the node's presence is what marks a
 * commit as indexed (the dedup key), so a failure between the two writes is retried — never skipped
 * — on the next run. The embedding call happens outside any database transaction, like the rest of
 * indexing.
 */
@Service
public class CommitIndexingService {

  private static final Logger log = LoggerFactory.getLogger(CommitIndexingService.class);

  /** Longest message prefix sent to the embedding model; the node keeps the full message. */
  private static final int EMBEDDED_MESSAGE_CHARS = 8_192;

  private final List<CommitHistoryPort> historyPorts;
  private final CommitRepository commits;
  private final EmbeddingPort embedding;
  private final VectorStorePort vectorStore;
  private final int historyDepth;

  public CommitIndexingService(
      List<CommitHistoryPort> historyPorts,
      CommitRepository commits,
      EmbeddingPort embedding,
      VectorStorePort vectorStore,
      AppProperties properties) {
    this.historyPorts = historyPorts;
    this.commits = commits;
    this.embedding = embedding;
    this.vectorStore = vectorStore;
    this.historyDepth = properties.commits().historyDepth();
  }

  /**
   * Indexes the source's commits newer than {@code sinceSha}, up to the configured history depth. A
   * source type with no history (a plain folder) or a depth of zero indexes nothing.
   *
   * @param source the source whose history to index
   * @param sinceSha the last commit indexed before, or {@code null} to read up to the depth limit
   * @return how many commits were newly indexed
   */
  public int index(Source source, String sinceSha) {
    if (historyDepth == 0) {
      return 0;
    }
    CommitHistoryPort port =
        historyPorts.stream().filter(p -> p.supports(source.type())).findFirst().orElse(null);
    if (port == null) {
      return 0;
    }

    List<CommitRecord> history = port.history(source, sinceSha, historyDepth);
    if (history.isEmpty()) {
      return 0;
    }
    Set<String> known =
        commits.existingShas(source.sourceId(), history.stream().map(CommitRecord::sha).toList());
    List<CommitRecord> fresh =
        history.stream().filter(commit -> !known.contains(commit.sha())).toList();
    if (fresh.isEmpty()) {
      return 0;
    }

    String ref = source.ref().orElse(null);
    Instant indexedAt = Instant.now();
    vectorStore.upsert(embedMessages(source.sourceId(), ref, fresh));
    commits.upsertAll(source.sourceId(), ref, indexedAt, fresh);
    log.info("Indexed {} commits for {}", fresh.size(), source.sourceId());
    return fresh.size();
  }

  /** Embeds each commit's message (its hash when the message is blank) in one batch call. */
  private List<ChunkVector> embedMessages(String sourceId, String ref, List<CommitRecord> fresh) {
    List<String> texts =
        fresh.stream()
            .map(commit -> commit.message().isBlank() ? commit.sha() : commit.message())
            .map(
                text ->
                    text.length() > EMBEDDED_MESSAGE_CHARS
                        ? text.substring(0, EMBEDDED_MESSAGE_CHARS)
                        : text)
            .toList();
    List<float[]> embeddings = embedding.embedBatch(texts);

    List<ChunkVector> vectors = new ArrayList<>(fresh.size());
    for (int i = 0; i < fresh.size(); i++) {
      CommitRecord commit = fresh.get(i);
      vectors.add(
          new ChunkVector(
              IdFactory.commitId(sourceId, commit.sha()),
              embeddings.get(i),
              metadata(sourceId, ref, commit)));
    }
    return vectors;
  }

  private static Map<String, Object> metadata(String sourceId, String ref, CommitRecord commit) {
    Map<String, Object> metadata = new HashMap<>();
    metadata.put("source_id", sourceId);
    metadata.put("type", "commit");
    metadata.put("commit_sha", commit.sha());
    if (ref != null) {
      metadata.put("ref", ref);
    }
    return metadata;
  }
}
