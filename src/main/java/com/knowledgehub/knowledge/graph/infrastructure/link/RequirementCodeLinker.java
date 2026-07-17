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
 *
 * <p>Example — a chunk reading <em>"REQ-12 is implemented by com.example.parse.PdfReader, verified
 * by src/test/java/com/example/parse/PdfReaderTests.java"</em> proposes two candidates from that
 * chunk, both at 0.85: {@code IMPLEMENTED_BY} to the {@code PdfReader} class (qualified name, not a
 * test), and {@code VERIFIED_BY} to each entity of the test file (path under {@code /test/} and
 * name ending in {@code Tests}). The same sentence without any requirement identifier would produce
 * nothing here — only the identifier turns a mention into a requirement claim.
 */
@Component
class RequirementCodeLinker extends AbstractDocumentLinker {

  /** A requirement identifier like {@code FR-3.1} or {@code NFR-12} (uppercase tag, number). */
  private static final Pattern REQUIREMENT_ID = Pattern.compile("\\b[A-Z]{2,}-\\d+(?:\\.\\d+)*\\b");

  private static final double REFERENCE_CONFIDENCE = 0.85;

  private final EntityResolver resolver;

  RequirementCodeLinker(EntityResolver resolver, SourceLanguages languages) {
    super(languages);
    this.resolver = resolver;
  }

  /**
   * Keeps only the document chunks that carry a requirement identifier, gathers every qualified
   * name and source-file path they mention, resolves both sets in two batched lookups, and returns
   * one candidate per (chunk, entity) pair — typed {@code VERIFIED_BY} or {@code IMPLEMENTED_BY} by
   * whether the reference looks like a test. Artifacts whose chunks carry no requirement identifier
   * yield an empty list.
   */
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
      addMatches(languages.qualifiedNamePattern(), chunk.text(), qualifiedNames);
      addMatches(languages.codePathPattern(), chunk.text(), paths);
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

  /** One candidate per qualified name in the chunk that resolved to exactly one entity. */
  private void qualifiedCandidates(
      Chunk chunk, Map<String, String> resolved, Set<String> linked, List<LinkCandidate> out) {
    Matcher matcher = languages.qualifiedNamePattern().matcher(chunk.text());
    while (matcher.find()) {
      String fqn = matcher.group();
      String toId = resolved.get(fqn);
      if (toId != null && linked.add(toId)) {
        out.add(new LinkCandidate(chunk.chunkId(), toId, typeFor(fqn), REFERENCE_CONFIDENCE, fqn));
      }
    }
  }

  /** One candidate per entity declared in each source-file path the chunk mentions. */
  private void pathCandidates(
      Chunk chunk, Map<String, List<String>> byPath, Set<String> linked, List<LinkCandidate> out) {
    Matcher matcher = languages.codePathPattern().matcher(chunk.text());
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
  private RelationType typeFor(String reference) {
    return languages.isTestReference(reference)
        ? RelationType.VERIFIED_BY
        : RelationType.IMPLEMENTED_BY;
  }
}
