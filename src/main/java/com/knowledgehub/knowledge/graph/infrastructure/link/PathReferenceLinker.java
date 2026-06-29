package com.knowledgehub.knowledge.graph.infrastructure.link;

import com.knowledgehub.knowledge.graph.domain.CrossArtifactLinker;
import com.knowledgehub.knowledge.graph.domain.EntityResolver;
import com.knowledgehub.knowledge.graph.domain.LinkCandidate;
import com.knowledgehub.knowledge.graph.domain.RelationType;
import com.knowledgehub.knowledge.graph.domain.ResolutionScope;
import com.knowledgehub.knowledge.indexing.domain.Chunk;
import com.knowledgehub.knowledge.indexing.domain.ChunkType;
import com.knowledgehub.knowledge.ingestion.domain.RawArtifact;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

/**
 * Proposes {@code DESCRIBES} links from a document chunk to the code declared in a source file it
 * names by path (e.g. a README pointing at {@code src/main/java/com/example/Greeter.java}). An
 * explicit path is strong, unambiguous evidence, so matches score high. Resolution is by the exact
 * stored file path, preferring the document's own source and widening across sources.
 */
@Component
class PathReferenceLinker implements CrossArtifactLinker {

  /** A path token ending in a Java source file. */
  private static final Pattern JAVA_PATH = Pattern.compile("\\b[\\w./-]*\\w+\\.java\\b");

  private static final double PATH_CONFIDENCE = 0.85;

  private final EntityResolver resolver;

  PathReferenceLinker(EntityResolver resolver) {
    this.resolver = resolver;
  }

  @Override
  public boolean supports(RawArtifact artifact) {
    return artifact.text() != null && !artifact.path().toLowerCase(Locale.ROOT).endsWith(".java");
  }

  @Override
  public List<LinkCandidate> link(RawArtifact artifact, List<Chunk> chunks) {
    ResolutionScope scope = new ResolutionScope(artifact.provenance().sourceId());
    List<LinkCandidate> candidates = new ArrayList<>();
    for (Chunk chunk : chunks) {
      if (chunk.type() != ChunkType.DOC) {
        continue;
      }
      Set<String> linked = new HashSet<>();
      Matcher matcher = JAVA_PATH.matcher(chunk.text());
      while (matcher.find()) {
        String path = matcher.group();
        for (String toId : resolver.findByPath(path, scope)) {
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
