package com.knowledgehub.knowledge.graph.infrastructure.link;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * The source languages the cross-artifact linkers know about. Registering a language here is the
 * single step that teaches every document linker about it: its files stop being treated as
 * documents, its file paths are recognised in prose, and its test naming conventions are
 * recognised. Structural extraction is separate — a new language there is a new {@code
 * StructuralExtractor} implementation.
 */
enum SourceLanguage {
  JAVA("java", List.of("Test", "Tests", "Spec", "IT"));

  /** A path token in prose ending in any known source-file extension. */
  private static final Pattern CODE_PATH =
      Pattern.compile(
          "\\b[\\w./-]*\\w+\\.(?:"
              + Arrays.stream(values())
                  .map(language -> language.extension)
                  .collect(Collectors.joining("|"))
              + ")\\b");

  private final String extension;
  private final List<String> testNameSuffixes;

  SourceLanguage(String extension, List<String> testNameSuffixes) {
    this.extension = extension;
    this.testNameSuffixes = testNameSuffixes;
  }

  /** Whether the path is a source file of any known language. */
  static boolean isCodePath(String path) {
    String lower = path.toLowerCase(Locale.ROOT);
    return Arrays.stream(values()).anyMatch(language -> lower.endsWith("." + language.extension));
  }

  /** Pattern matching a source-file path of any known language inside prose. */
  static Pattern codePathPattern() {
    return CODE_PATH;
  }

  /**
   * Whether a referenced path or type name follows a known test convention — a test source root in
   * the path, or a type name carrying one of a language's test suffixes.
   */
  static boolean isTestReference(String reference) {
    if (reference.toLowerCase(Locale.ROOT).contains("/test/")) {
      return true;
    }
    String name = simpleName(reference);
    return Arrays.stream(values())
        .flatMap(language -> language.testNameSuffixes.stream())
        .anyMatch(name::endsWith);
  }

  /** The simple type name: the last path/package segment, minus any known source extension. */
  private static String simpleName(String reference) {
    String trimmed = reference;
    String lower = reference.toLowerCase(Locale.ROOT);
    for (SourceLanguage language : values()) {
      String suffix = "." + language.extension;
      if (lower.endsWith(suffix)) {
        trimmed = reference.substring(0, reference.length() - suffix.length());
        break;
      }
    }
    int separator = Math.max(trimmed.lastIndexOf('/'), trimmed.lastIndexOf('.'));
    return trimmed.substring(separator + 1);
  }
}
