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
 * Proposes {@code DESCRIBES} links from a document chunk to the code it names. A fully-qualified
 * reference resolved to one entity is strong evidence; a <em>compound</em> CamelCase name (e.g.
 * {@code CodeChunker}) matched to a single entity is weaker; everything else - a name matching
 * several entities, or a single-word name like {@code Chunk} or {@code Context} that doubles as
 * ordinary prose - is scored low so the confidence threshold drops it by default. The linker only
 * scores; keeping or dropping is the caller's policy.
 *
 * <p>All names found across the artifact's chunks are resolved in two batched lookups (one for
 * qualified names, one for bare names), so a document is a couple of queries regardless of length.
 *
 * <p>Example — a chunk reading <em>"CodeChunker cuts code by AST; see
 * com.example.chunking.DocAnalyzer; every Chunk keeps its text"</em> proposes three DESCRIBES
 * candidates: to {@code DocAnalyzer} at 0.9 (qualified name, one match), to {@code CodeChunker} at
 * 0.7 (compound bare name, one match), and to {@code Chunk} at 0.4 (single word that is also
 * ordinary prose) — the last falls under the default confidence threshold and never reaches the
 * graph.
 */
@Component
class IdentifierMatchLinker extends AbstractDocumentLinker {

  private static final double QUALIFIED_CONFIDENCE = 0.9;
  private static final double UNIQUE_NAME_CONFIDENCE = 0.7;
  private static final double WEAK_NAME_CONFIDENCE = 0.4;

  private final EntityResolver resolver;

  IdentifierMatchLinker(EntityResolver resolver, SourceLanguages languages) {
    super(languages);
    this.resolver = resolver;
  }

  /**
   * Scans the document chunks for identifier mentions and turns them into scored candidates. Two
   * passes over each chunk: qualified names first (strongest signal), then bare CamelCase names; a
   * per-chunk seen-set keeps one candidate per (chunk, entity) pair, so an entity mentioned both
   * ways keeps only its strongest score. Returns an empty list for an artifact with no document
   * chunks or no resolvable mention.
   */
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
      addMatches(languages.qualifiedNamePattern(), chunk.text(), qualifiedNames);
      addMatches(languages.bareNamePattern(), chunk.text(), bareNames);
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

  /**
   * One candidate per qualified mention that resolved to exactly one entity — {@code
   * com.example.chunking.DocAnalyzer} in the chunk text becomes {@code (chunk DESCRIBES
   * DocAnalyzer, 0.9, evidence = the name)}. Targets are recorded in {@code linked} so the weaker
   * bare-name pass skips them.
   */
  private void qualifiedCandidates(
      Chunk chunk, Map<String, String> resolved, Set<String> linked, List<LinkCandidate> out) {
    Matcher matcher = languages.qualifiedNamePattern().matcher(chunk.text());
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

  /**
   * One candidate per bare CamelCase mention, scored by how convincing the match is: a compound
   * name matching exactly one entity ({@code CodeChunker}) scores 0.7; a single-word name ({@code
   * Chunk}) or a name matching several entities scores 0.4 and is expected to fall under the
   * threshold. When several entities match, the source-preferred first one is proposed.
   */
  private void bareNameCandidates(
      Chunk chunk, Map<String, List<String>> byName, Set<String> linked, List<LinkCandidate> out) {
    Matcher matcher = languages.bareNamePattern().matcher(chunk.text());
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
      boolean strong = matches.size() == 1 && languages.isCompoundName(name);
      double score = strong ? UNIQUE_NAME_CONFIDENCE : WEAK_NAME_CONFIDENCE;
      out.add(new LinkCandidate(chunk.chunkId(), toId, RelationType.DESCRIBES, score, name));
    }
  }
}
