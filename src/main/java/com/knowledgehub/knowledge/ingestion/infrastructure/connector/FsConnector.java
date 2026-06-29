package com.knowledgehub.knowledge.ingestion.infrastructure.connector;

import com.knowledgehub.knowledge.ingestion.domain.Connector;
import com.knowledgehub.knowledge.ingestion.domain.FsProvenance;
import com.knowledgehub.knowledge.ingestion.domain.RawArtifact;
import com.knowledgehub.knowledge.ingestion.domain.Source;
import com.knowledgehub.knowledge.ingestion.domain.SourceType;
import com.knowledgehub.knowledge.ingestion.infrastructure.MediaTypes;
import com.knowledgehub.shared.id.Hashing;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Optional;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Connector for filesystem sources: walks a folder, applies include/ignore globs, and emits one
 * {@link RawArtifact} per file with {@link FsProvenance}. Symlinks and oversized files are skipped
 * (path-traversal / memory safety); an unreadable file is logged and skipped, never aborting the
 * walk.
 */
@Component
class FsConnector implements Connector {

  private static final Logger log = LoggerFactory.getLogger(FsConnector.class);
  private static final long MAX_FILE_BYTES = 5L * 1024 * 1024;

  @Override
  public boolean supports(SourceType type) {
    return type == SourceType.FS;
  }

  @Override
  public Stream<RawArtifact> fetch(Source source) {
    Path root = Path.of(source.uriOrPath()).toAbsolutePath().normalize();
    if (!Files.isDirectory(root)) {
      throw new IllegalArgumentException("Filesystem source path is not a directory: " + root);
    }
    GlobMatcher matcher = GlobMatcher.forSource(source);
    Instant indexedAt = Instant.now();
    try {
      return Files.walk(root)
          .filter(Files::isRegularFile)
          .filter(path -> !Files.isSymbolicLink(path))
          .filter(path -> matcher.accepts(relativePath(root, path)))
          .map(path -> readArtifact(source, root, path, indexedAt))
          .flatMap(Optional::stream);
    } catch (IOException e) {
      throw new UncheckedIOException("Failed to walk filesystem source " + root, e);
    }
  }

  private Optional<RawArtifact> readArtifact(
      Source source, Path root, Path file, Instant indexedAt) {
    String relative = relativePath(root, file);
    try {
      long size = Files.size(file);
      if (size > MAX_FILE_BYTES) {
        log.warn(
            "Skipping oversized file {} ({} bytes) in source {}",
            relative,
            size,
            source.sourceId());
        return Optional.empty();
      }
      byte[] content = Files.readAllBytes(file);
      FsProvenance provenance =
          new FsProvenance(source.sourceId(), relative, Hashing.sha256(content), indexedAt);
      return Optional.of(
          RawArtifact.raw(relative, MediaTypes.fromPath(relative), content, provenance));
    } catch (IOException e) {
      log.warn(
          "Skipping unreadable file {} in source {}: {}",
          relative,
          source.sourceId(),
          e.toString());
      return Optional.empty();
    }
  }

  private static String relativePath(Path root, Path file) {
    return root.relativize(file).toString().replace('\\', '/');
  }
}
