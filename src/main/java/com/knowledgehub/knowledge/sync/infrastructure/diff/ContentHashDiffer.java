package com.knowledgehub.knowledge.sync.infrastructure.diff;

import com.knowledgehub.knowledge.ingestion.domain.Connector;
import com.knowledgehub.knowledge.ingestion.domain.GitProvenance;
import com.knowledgehub.knowledge.ingestion.domain.RawArtifact;
import com.knowledgehub.knowledge.ingestion.domain.Source;
import com.knowledgehub.knowledge.sync.domain.ChangeSet;
import com.knowledgehub.knowledge.sync.domain.SourceDiffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.stereotype.Component;

/**
 * Diffs a source by content hash, the same way for git and filesystem alike: it lists the source's
 * current files (via the ingestion connector, which carries each file's content hash) and compares
 * them to the hashes already stored on the {@code :File} nodes. A path present only now is added,
 * one whose hash changed is modified, and one stored but no longer present is deleted.
 *
 * <p>This needs no source-type-specific logic, so it serves both source types; a git source could
 * later swap in a cheaper commit-diff implementation behind the same port.
 */
@Component
class ContentHashDiffer implements SourceDiffer {

  private static final String STORED_HASHES =
      "MATCH (f:File {source_id: $sourceId}) RETURN f.path AS path, f.content_hash AS hash";

  private final List<Connector> connectors;
  private final Neo4jClient client;

  ContentHashDiffer(List<Connector> connectors, Neo4jClient client) {
    this.connectors = connectors;
    this.client = client;
  }

  @Override
  public boolean supports(Source source) {
    return connectors.stream().anyMatch(c -> c.supports(source.type()));
  }

  @Override
  public ChangeSet diff(Source source) {
    Map<String, String> stored = storedHashes(source.sourceId());
    Current current = currentState(source);

    List<String> added = new ArrayList<>();
    List<String> modified = new ArrayList<>();
    current.hashes.forEach(
        (path, hash) -> {
          String previous = stored.get(path);
          if (previous == null) {
            added.add(path);
          } else if (!previous.equals(hash)) {
            modified.add(path);
          }
        });

    List<String> deleted =
        stored.keySet().stream().filter(path -> !current.hashes.containsKey(path)).toList();

    return new ChangeSet(source.sourceId(), added, modified, deleted, current.commitSha);
  }

  private Map<String, String> storedHashes(String sourceId) {
    Map<String, String> hashes = new HashMap<>();
    client
        .query(STORED_HASHES)
        .bind(sourceId)
        .to("sourceId")
        .fetch()
        .all()
        .forEach(row -> hashes.put((String) row.get("path"), (String) row.get("hash")));
    return hashes;
  }

  /** Reads the source's current files: each path's content hash and the commit, if it is git. */
  private Current currentState(Source source) {
    Connector connector =
        connectors.stream()
            .filter(c -> c.supports(source.type()))
            .findFirst()
            .orElseThrow(
                () -> new IllegalStateException("No connector for source type " + source.type()));

    Map<String, String> hashes = new HashMap<>();
    String[] commitSha = {null};
    try (Stream<RawArtifact> artifacts = connector.fetch(source)) {
      artifacts.forEach(
          artifact -> {
            hashes.put(artifact.path(), artifact.provenance().contentHash());
            if (artifact.provenance() instanceof GitProvenance git) {
              commitSha[0] = git.commitSha();
            }
          });
    }
    return new Current(hashes, commitSha[0]);
  }

  private record Current(Map<String, String> hashes, String commitSha) {}
}
