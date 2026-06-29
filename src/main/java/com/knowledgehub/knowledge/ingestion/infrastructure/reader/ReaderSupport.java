package com.knowledgehub.knowledge.ingestion.infrastructure.reader;

import com.knowledgehub.knowledge.ingestion.domain.RawArtifact;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.ai.document.Document;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;

/** Shared helpers for the Spring AI-backed document readers. */
final class ReaderSupport {

  private ReaderSupport() {}

  /** Wraps an artifact's content bytes as a Spring {@link Resource} for a Spring AI reader. */
  static Resource resource(RawArtifact artifact) {
    return new ByteArrayResource(artifact.content());
  }

  /** Joins the text of parsed documents into a single block, blank-trimmed. */
  static String joinText(List<Document> documents) {
    return documents.stream()
        .map(Document::getText)
        .filter(Objects::nonNull)
        .collect(Collectors.joining("\n\n"))
        .strip();
  }
}
