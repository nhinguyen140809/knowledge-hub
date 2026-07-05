package com.knowledgehub.knowledge.indexing.infrastructure.chunking;

import com.knowledgehub.knowledge.domain.SourceLanguage;
import com.knowledgehub.knowledge.indexing.domain.Chunker;
import com.knowledgehub.knowledge.ingestion.domain.RawArtifact;
import java.util.Locale;

/**
 * Shared base for language-specific code chunkers: it binds the chunker to one {@link
 * SourceLanguage} and accepts an artifact only when the artifact carries text and its path ends in
 * that language's source extension. Each language attaches its own subclass (e.g. {@code
 * JavaCodeChunker}) carrying the language-specific parser; the extension is never hard-coded here
 * or in the subclass but read from the registered {@link SourceLanguage}, so it stays the single
 * source of truth. Subclasses register at highest precedence so a code file is chunked by its
 * language strategy before falling through to the document chunker.
 */
public abstract class AbstractCodeChunker implements Chunker {

  private final String extension;

  protected AbstractCodeChunker(SourceLanguage language) {
    this.extension = "." + language.fileExtension().toLowerCase(Locale.ROOT);
  }

  @Override
  public boolean supports(RawArtifact artifact) {
    return artifact.text() != null && artifact.path().toLowerCase(Locale.ROOT).endsWith(extension);
  }
}
