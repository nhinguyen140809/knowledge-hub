package com.knowledgehub.knowledge.graph.infrastructure.link;

import com.knowledgehub.knowledge.graph.domain.EntityResolver;
import com.knowledgehub.knowledge.graph.domain.LinkCandidate;
import com.knowledgehub.knowledge.graph.domain.RelationType;
import com.knowledgehub.knowledge.graph.domain.ResolutionScope;
import com.knowledgehub.knowledge.indexing.domain.Chunk;
import com.knowledgehub.knowledge.ingestion.domain.RawArtifact;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

/**
 * Proposes {@code DESCRIBES} links from a document chunk to the code declared in a source file it
 * names by path (e.g. a README pointing at {@code src/main/java/com/example/Greeter.java}). An
 * explicit path is strong, unambiguous evidence, so matches score high. All paths found across the
 * artifact's chunks are resolved in one batched lookup against the exact stored file path,
 * preferring the document's own source and widening across sources.
 */
@Component
class PathReferenceLinker extends AbstractDocumentLinker {

  /** A path token ending in a Java source file. */
  private static final Pattern JAVA_PATH = Pattern.compile("\\b[\\w./-]*\\w+\\.java\\b");

  private static final double PATH_CONFIDENCE = 0.85;

  private final EntityResolver resolver;

  PathReferenceLinker(EntityResolver resolver) {
    this.resolver = resolver;
  }

  @Override
  public List<LinkCandidate> link(RawArtifact artifact, List<Chunk> chunks) {
    List<Chunk> docs = documentChunks(chunks);
    if (docs.isEmpty()) {
      return List.of();
    }
    ResolutionScope scope = new ResolutionScope(artifact.provenance().sourceId());

    Set<String> paths = new LinkedHashSet<>();
    for (Chunk chunk : docs) {
      Matcher matcher = JAVA_PATH.matcher(chunk.text());
      while (matcher.find()) {
        paths.add(matcher.group());
      }
    }
    Map<String, List<String>> byPath = resolver.findByPath(paths, scope);

    List<LinkCandidate> candidates = new ArrayList<>();
    for (Chunk chunk : docs) {
      Set<String> linked = new HashSet<>();
      Matcher matcher = JAVA_PATH.matcher(chunk.text());
      while (matcher.find()) {
        String path = matcher.group();
        for (String toId : byPath.getOrDefault(path, List.of())) {
          if (linked.add(toId)) {
            candidates.add(
                new LinkCandidate(
                    chunk.chunkId(), toId, RelationType.DESCRIBES, PATH_CONFIDENCE, path));
          }
        }
      }
    }
    return candidates;
  }
}
