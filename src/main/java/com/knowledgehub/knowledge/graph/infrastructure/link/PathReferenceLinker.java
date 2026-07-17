package com.knowledgehub.knowledge.graph.infrastructure.link;

import com.knowledgehub.knowledge.analysis.domain.Chunk;
import com.knowledgehub.knowledge.domain.RelationType;
import com.knowledgehub.knowledge.graph.domain.LinkCandidate;
import com.knowledgehub.knowledge.graph.domain.ResolutionScope;
import com.knowledgehub.knowledge.graph.domain.port.EntityResolver;
import com.knowledgehub.knowledge.infrastructure.lang.SourceLanguages;
import com.knowledgehub.knowledge.ingestion.domain.RawArtifact;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import org.springframework.stereotype.Component;

/**
 * Proposes {@code DESCRIBES} links from a document chunk to the code declared in a source file it
 * names by path (e.g. a README pointing at {@code src/main/java/com/example/Greeter.java}). An
 * explicit path is strong, unambiguous evidence, so matches score high. All paths found across the
 * artifact's chunks are resolved in one batched lookup against the exact stored file path,
 * preferring the document's own source and widening across sources.
 *
 * <p>Example — a chunk reading <em>"the parser lives in src/reader/PdfReader.java"</em>: if that
 * path is an indexed file declaring class {@code PdfReader} with two methods, the chunk gets a
 * DESCRIBES candidate to each of those entities at 0.85. A path that matches no indexed file — a
 * typo, or a file from a source not yet indexed — produces nothing rather than a guess; the next
 * re-link picks it up once the file exists.
 */
@Component
class PathReferenceLinker extends AbstractDocumentLinker {

  private static final double PATH_CONFIDENCE = 0.85;

  private final EntityResolver resolver;

  PathReferenceLinker(EntityResolver resolver, SourceLanguages languages) {
    super(languages);
    this.resolver = resolver;
  }

  /**
   * Gathers every source-file path mentioned across the document chunks, resolves them to declared
   * entities in one batched lookup, and returns one candidate per (chunk, entity) pair. Returns an
   * empty list when the artifact has no document chunks or none of its paths resolve.
   */
  @Override
  public List<LinkCandidate> link(RawArtifact artifact, List<Chunk> chunks) {
    List<Chunk> docs = documentChunks(chunks);
    if (docs.isEmpty()) {
      return List.of();
    }
    ResolutionScope scope = new ResolutionScope(artifact.provenance().sourceId());

    Set<String> paths = new LinkedHashSet<>();
    for (Chunk chunk : docs) {
      Matcher matcher = languages.codePathPattern().matcher(chunk.text());
      while (matcher.find()) {
        paths.add(matcher.group());
      }
    }
    Map<String, List<String>> byPath = resolver.findByPath(paths, scope);

    List<LinkCandidate> candidates = new ArrayList<>();
    for (Chunk chunk : docs) {
      Set<String> linked = new HashSet<>();
      Matcher matcher = languages.codePathPattern().matcher(chunk.text());
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
