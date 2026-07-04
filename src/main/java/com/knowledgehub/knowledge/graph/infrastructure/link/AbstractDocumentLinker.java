package com.knowledgehub.knowledge.graph.infrastructure.link;

import com.knowledgehub.knowledge.graph.domain.CrossArtifactLinker;
import com.knowledgehub.knowledge.indexing.domain.Chunk;
import com.knowledgehub.knowledge.indexing.domain.ChunkType;
import com.knowledgehub.knowledge.infrastructure.lang.SourceLanguages;
import com.knowledgehub.knowledge.ingestion.domain.RawArtifact;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Shared base for cross-artifact linkers that read prose out of document chunks: it applies to any
 * artifact that carries text and is not a known source language (see {@link
 * com.knowledgehub.knowledge.domain.SourceLanguage}), and exposes the artifact's document chunks.
 * Each subclass looks for one kind of signal (a named identifier, a path reference) over those
 * chunks.
 */
abstract class AbstractDocumentLinker implements CrossArtifactLinker {

  /** The registered languages — how code files, references and test names are recognised. */
  protected final SourceLanguages languages;

  protected AbstractDocumentLinker(SourceLanguages languages) {
    this.languages = languages;
  }

  /**
   * A document is any artifact that carries text and is not a known source language: {@code
   * README.md} or {@code notes.txt} qualify, {@code Greeter.java} does not — code structure is the
   * structural extractor's job, and scanning code as prose would flood the linkers with false
   * names.
   */
  @Override
  public boolean supports(RawArtifact artifact) {
    return artifact.text() != null && !languages.isCodePath(artifact.path());
  }

  /**
   * Narrows the artifact's chunks to the DOC-typed ones — the only text a cross-artifact linker
   * reads, and the from-end of every candidate it proposes.
   *
   * @param chunks all chunks of the artifact, as produced by chunking
   * @return only the document chunks, in their original order
   */
  protected static List<Chunk> documentChunks(List<Chunk> chunks) {
    return chunks.stream().filter(chunk -> chunk.type() == ChunkType.DOC).toList();
  }

  /**
   * Collects every distinct match of {@code pattern} in {@code text} into {@code into} — e.g. the
   * qualified-name pattern over "see com.example.Greeter and com.example.Clock" adds those two
   * names. Used to gather all references first so they resolve in one batched lookup.
   */
  protected static void addMatches(Pattern pattern, String text, Set<String> into) {
    Matcher matcher = pattern.matcher(text);
    while (matcher.find()) {
      into.add(matcher.group());
    }
  }
}
