package com.knowledgehub.knowledge.graph.infrastructure.link;

import com.knowledgehub.knowledge.graph.domain.CrossArtifactLinker;
import com.knowledgehub.knowledge.indexing.domain.Chunk;
import com.knowledgehub.knowledge.indexing.domain.ChunkType;
import com.knowledgehub.knowledge.ingestion.domain.RawArtifact;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Shared base for cross-artifact linkers that read prose out of document chunks: it applies to any
 * artifact that carries text and is not a known source language (see {@link SourceLanguage}), and
 * exposes the artifact's document chunks. Each subclass looks for one kind of signal (a named
 * identifier, a path reference) over those chunks.
 */
abstract class AbstractDocumentLinker implements CrossArtifactLinker {

  @Override
  public boolean supports(RawArtifact artifact) {
    return artifact.text() != null && !SourceLanguage.isCodePath(artifact.path());
  }

  /** The artifact's document chunks - the only chunks a cross-artifact linker reads. */
  protected static List<Chunk> documentChunks(List<Chunk> chunks) {
    return chunks.stream().filter(chunk -> chunk.type() == ChunkType.DOC).toList();
  }

  /** Collects every match of {@code pattern} in {@code text} into {@code into}. */
  protected static void addMatches(Pattern pattern, String text, Set<String> into) {
    Matcher matcher = pattern.matcher(text);
    while (matcher.find()) {
      into.add(matcher.group());
    }
  }
}
