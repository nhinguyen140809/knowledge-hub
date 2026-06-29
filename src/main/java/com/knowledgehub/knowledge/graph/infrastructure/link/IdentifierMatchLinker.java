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
 * Proposes {@code DESCRIBES} links from a document chunk to the code it names. A fully-qualified
 * reference resolved to one entity is strong evidence; a bare type name matched to a single entity
 * is weaker; a name that matches several entities is ambiguous and scored low so the confidence
 * threshold drops it. The linker only scores — keeping or dropping is the caller's policy.
 */
@Component
class IdentifierMatchLinker implements CrossArtifactLinker {

  /**
   * A qualified reference like {@code com.example.Greeter} (lowercase packages, capitalised type).
   */
  private static final Pattern QUALIFIED =
      Pattern.compile("\\b(?:[a-z][\\w]*\\.)+[A-Z][A-Za-z0-9]*\\b");

  /** A bare CamelCase type name like {@code Greeter} (capitalised, contains a lowercase letter). */
  private static final Pattern CAMEL_CASE =
      Pattern.compile("\\b[A-Z][A-Za-z0-9]*[a-z][A-Za-z0-9]*\\b");

  private static final double QUALIFIED_CONFIDENCE = 0.9;
  private static final double UNIQUE_NAME_CONFIDENCE = 0.7;
  private static final double AMBIGUOUS_NAME_CONFIDENCE = 0.4;

  private final EntityResolver resolver;

  IdentifierMatchLinker(EntityResolver resolver) {
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
      Set<String> linkedTargets = new HashSet<>();
      qualifiedRefs(chunk, scope, candidates, linkedTargets);
      bareNameRefs(chunk, scope, candidates, linkedTargets);
    }
    return candidates;
  }

  private void qualifiedRefs(
      Chunk chunk, ResolutionScope scope, List<LinkCandidate> out, Set<String> linked) {
    Matcher matcher = QUALIFIED.matcher(chunk.text());
    while (matcher.find()) {
      String fqn = matcher.group();
      resolver
          .resolve(fqn, scope)
          .filter(linked::add)
          .ifPresent(
              toId ->
                  out.add(
                      new LinkCandidate(
                          chunk.chunkId(),
                          toId,
                          RelationType.DESCRIBES,
                          QUALIFIED_CONFIDENCE,
                          fqn)));
    }
  }

  private void bareNameRefs(
      Chunk chunk, ResolutionScope scope, List<LinkCandidate> out, Set<String> linked) {
    Matcher matcher = CAMEL_CASE.matcher(chunk.text());
    while (matcher.find()) {
      String name = matcher.group();
      List<String> matches = resolver.findByName(name, scope);
      if (matches.isEmpty()) {
        continue;
      }
      String toId = matches.get(0);
      if (!linked.add(toId)) {
        continue;
      }
      double score = matches.size() == 1 ? UNIQUE_NAME_CONFIDENCE : AMBIGUOUS_NAME_CONFIDENCE;
      out.add(new LinkCandidate(chunk.chunkId(), toId, RelationType.DESCRIBES, score, name));
    }
  }
}
