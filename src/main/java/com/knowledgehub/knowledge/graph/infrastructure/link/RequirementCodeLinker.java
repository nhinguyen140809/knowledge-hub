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
 * Links a requirement to the code that fulfils it. A document chunk that carries a requirement
 * identifier (e.g. {@code FR-3} or {@code NFR-12}) and names code by fully-qualified name or by
 * file path is read as a requirement pointing at its implementation: the link is {@code
 * VERIFIED_BY} when the target is a test and {@code IMPLEMENTED_BY} otherwise.
 *
 * <p>Only strong, explicit references (a qualified name or a path) count here, because a
 * requirement-to-code claim is high-stakes; loose name mentions stay the province of {@code
 * DESCRIBES}. Chunks without a requirement identifier produce nothing. All references across the
 * artifact's requirement chunks are resolved in two batched lookups.
 */
@Component
class RequirementCodeLinker extends AbstractDocumentLinker {

  /** A requirement identifier like {@code FR-3.1} or {@code NFR-12} (uppercase tag, number). */
  private static final Pattern REQUIREMENT_ID = Pattern.compile("\\b[A-Z]{2,}-\\d+(?:\\.\\d+)*\\b");

  /** A qualified reference like {@code com.example.Greeter}. */
  private static final Pattern QUALIFIED =
      Pattern.compile("\\b(?:[a-z][\\w]*\\.)+[A-Z][A-Za-z0-9]*\\b");

  /** A path token ending in a source file of any known language. */
  private static final Pattern CODE_PATH = SourceLanguage.codePathPattern();

  private static final double REFERENCE_CONFIDENCE = 0.85;

  private final EntityResolver resolver;

  RequirementCodeLinker(EntityResolver resolver) {
    this.resolver = resolver;
  }

  @Override
  public List<LinkCandidate> link(RawArtifact artifact, List<Chunk> chunks) {
    List<Chunk> requirementChunks =
        documentChunks(chunks).stream()
            .filter(chunk -> REQUIREMENT_ID.matcher(chunk.text()).find())
            .toList();
    if (requirementChunks.isEmpty()) {
      return List.of();
    }
    ResolutionScope scope = new ResolutionScope(artifact.provenance().sourceId());

    Set<String> qualifiedNames = new LinkedHashSet<>();
    Set<String> paths = new LinkedHashSet<>();
    for (Chunk chunk : requirementChunks) {
      addMatches(QUALIFIED, chunk.text(), qualifiedNames);
      addMatches(CODE_PATH, chunk.text(), paths);
    }
    Map<String, String> resolvedQualified = resolver.resolve(qualifiedNames, scope);
    Map<String, List<String>> byPath = resolver.findByPath(paths, scope);

    List<LinkCandidate> candidates = new ArrayList<>();
    for (Chunk chunk : requirementChunks) {
      Set<String> linked = new HashSet<>();
      qualifiedCandidates(chunk, resolvedQualified, linked, candidates);
      pathCandidates(chunk, byPath, linked, candidates);
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
        out.add(new LinkCandidate(chunk.chunkId(), toId, typeFor(fqn), REFERENCE_CONFIDENCE, fqn));
      }
    }
  }

  private static void pathCandidates(
      Chunk chunk, Map<String, List<String>> byPath, Set<String> linked, List<LinkCandidate> out) {
    Matcher matcher = CODE_PATH.matcher(chunk.text());
    while (matcher.find()) {
      String path = matcher.group();
      for (String toId : byPath.getOrDefault(path, List.of())) {
        if (linked.add(toId)) {
          out.add(
              new LinkCandidate(chunk.chunkId(), toId, typeFor(path), REFERENCE_CONFIDENCE, path));
        }
      }
    }
  }

  /** A reference naming a test verifies the requirement; anything else implements it. */
  private static RelationType typeFor(String reference) {
    return SourceLanguage.isTestReference(reference)
        ? RelationType.VERIFIED_BY
        : RelationType.IMPLEMENTED_BY;
  }
}
