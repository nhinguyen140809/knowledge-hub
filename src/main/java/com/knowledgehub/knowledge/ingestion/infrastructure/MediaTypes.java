package com.knowledgehub.knowledge.ingestion.infrastructure;

import java.util.Locale;
import java.util.Set;

/**
 * Maps a file path to the media type used to pick a {@code DocumentReader}. Code and plain-text
 * files keep their exact bytes (read verbatim); Markdown and PDF have dedicated readers; anything
 * else falls back to Tika.
 */
public final class MediaTypes {

  public static final String MARKDOWN = "text/markdown";
  public static final String PDF = "application/pdf";
  public static final String PLAIN_TEXT = "text/plain";
  public static final String OCTET_STREAM = "application/octet-stream";

  // Extensions read verbatim as UTF-8 text (source code, config, structured text). Kept exact so
  // the
  // downstream AST/structure-aware chunking sees the original bytes.
  private static final Set<String> TEXT_EXTENSIONS =
      Set.of(
          "txt",
          "java",
          "kt",
          "py",
          "js",
          "ts",
          "tsx",
          "jsx",
          "go",
          "rs",
          "c",
          "h",
          "cpp",
          "hpp",
          "cs",
          "rb",
          "php",
          "scala",
          "swift",
          "sh",
          "bash",
          "sql",
          "json",
          "yaml",
          "yml",
          "xml",
          "html",
          "css",
          "scss",
          "properties",
          "toml",
          "gradle",
          "cfg",
          "ini");

  private MediaTypes() {}

  /** Detects the media type from the path's extension. */
  public static String fromPath(String path) {
    String ext = extension(path);
    if (ext.equals("md") || ext.equals("markdown")) {
      return MARKDOWN;
    }
    if (ext.equals("pdf")) {
      return PDF;
    }
    if (TEXT_EXTENSIONS.contains(ext)) {
      return PLAIN_TEXT;
    }
    return OCTET_STREAM;
  }

  private static String extension(String path) {
    String lower = path.toLowerCase(Locale.ROOT);
    int dot = lower.lastIndexOf('.');
    int slash = Math.max(lower.lastIndexOf('/'), lower.lastIndexOf('\\'));
    return dot > slash ? lower.substring(dot + 1) : "";
  }
}
