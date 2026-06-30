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
 * Proposes {@code DESCRIBES} links from a document chunk to the code it names. A fully-qualified
 * reference resolved to one entity is strong evidence; a <em>compound</em> CamelCase name (e.g.
 * {@code CodeChunker}) matched to a single entity is weaker; everything else - a name matching
 * several entities, or a single-word name like {@code Chunk} or {@code Context} that doubles as
 * ordinary prose - is scored low so the confidence threshold drops it by default. The linker only
 * scores; keeping or dropping is the caller's policy.
 *
 * <p>All names found across the artifact's chunks are resolved in two batched lookups (one for
 * qualified names, one for bare names), so a document is a couple of queries regardless of length.
 */
@Component
class IdentifierMatchLinker extends AbstractDocumentLinker {

  /**
   * A qualified reference like {@code com.example.Greeter} (lowercase packages, capitalised type).
   */
  private static final Pattern QUALIFIED =
      Pattern.compile("\\b(?:[a-z][\\w]*\\.)+[A-Z][A-Za-z0-9]*\\b");

  /** A bare CamelCase type name like {@code Greeter} (capitalised, contains a lowercase letter). */
  private static final Pattern CAMEL_CASE =
      Pattern.compile("\\b[A-Z][A-Za-z0-9]*[a-z][A-Za-z0-9]*\\b");

  /** A second capitalised segment (e.g. the {@code C} in {@code CodeChunker}) marks a compound. */
  private static final Pattern COMPOUND = Pattern.compile("[a-z0-9][A-Z]");

  private static final double QUALIFIED_CONFIDENCE = 0.9;
  private static final double UNIQUE_NAME_CONFIDENCE = 0.7;
  private static final double WEAK_NAME_CONFIDENCE = 0.4;

  private final EntityResolver resolver;

  IdentifierMatchLinker(EntityResolver resolver) {
    this.resolver = resolver;
  }

  @Override
  public List<LinkCandidate> link(RawArtifact artifact, List<Chunk> chunks) {
    List<Chunk> docs = documentChunks(chunks);
    if (docs.isEmpty()) {
      return List.of();
    }
    ResolutionScope scope = new ResolutionScope(artifact.provenance().sourceId());

    Set<String> qualifiedNames = new LinkedHashSet<>();
    Set<String> bareNames = new LinkedHashSet<>();
    for (Chunk chunk : docs) {
      addMatches(QUALIFIED, chunk.text(), qualifiedNames);
      addMatches(CAMEL_CASE, chunk.text(), bareNames);
    }
    Map<String, String> resolvedQualified = resolver.resolve(qualifiedNames, scope);
    Map<String, List<String>> byName = resolver.findByName(bareNames, scope);

    List<LinkCandidate> candidates = new ArrayList<>();
    for (Chunk chunk : docs) {
      Set<String> linkedTargets = new HashSet<>();
      qualifiedCandidates(chunk, resolvedQualified, linkedTargets, candidates);
      bareNameCandidates(chunk, byName, linkedTargets, candidates);
    }
    return candidates;
  }

  private static void qualifiedCandidates(
      Chunk chunk, Map<String, String> resolved, Set<String> linked, List<LinkCandidate> out) {
    Matcher matcher = QUALIFIED.matcher(chunk.text());
    while (matcher.find()) {
      String fqn = matcher.group();
      String toId = resolved.get(fqn);
      if (toId != null && linked.add(toId)) {
        out.add(
            new LinkCandidate(
                chunk.chunkId(), toId, RelationType.DESCRIBES, QUALIFIED_CONFIDENCE, fqn));
      }
    }
  }

  private static void bareNameCandidates(
      Chunk chunk, Map<String, List<String>> byName, Set<String> linked, List<LinkCandidate> out) {
    Matcher matcher = CAMEL_CASE.matcher(chunk.text());
    while (matcher.find()) {
      String name = matcher.group();
      List<String> matches = byName.get(name);
      if (matches == null || matches.isEmpty()) {
        continue;
      }
      String toId = matches.get(0);
      if (!linked.add(toId)) {
        continue;
      }
      boolean strong = matches.size() == 1 && COMPOUND.matcher(name).find();
      double score = strong ? UNIQUE_NAME_CONFIDENCE : WEAK_NAME_CONFIDENCE;
      out.add(new LinkCandidate(chunk.chunkId(), toId, RelationType.DESCRIBES, score, name));
    }
  }
}
