package com.knowledgehub.knowledge.infrastructure.lang;

import com.knowledgehub.knowledge.domain.port.SourceLanguage;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * Every {@link SourceLanguage} the linkers know about, discovered by Spring, exposed as one view:
 * combined reference patterns, the code-file test, and the test-name conventions. The linkers only
 * talk to this registry, so a new language never touches them.
 */
@Component
public class SourceLanguages {

  /** Matches nothing — the safe combined pattern when no language is registered. */
  private static final String NOTHING = "(?!)";

  private final List<SourceLanguage> languages;
  private final Pattern codePath;
  private final Pattern qualifiedName;
  private final Pattern bareName;

  public SourceLanguages(List<SourceLanguage> languages) {
    this.languages = List.copyOf(languages);
    this.codePath =
        languages.isEmpty()
            ? Pattern.compile(NOTHING)
            : Pattern.compile(
                "\\b[\\w./-]*\\w+\\.(?:"
                    + languages.stream()
                        .map(SourceLanguage::fileExtension)
                        .collect(Collectors.joining("|"))
                    + ")\\b");
    this.qualifiedName = union(languages, SourceLanguage::qualifiedNamePattern);
    this.bareName = union(languages, SourceLanguage::bareNamePattern);
  }

  /** Whether the path is a source file of any known language. */
  public boolean isCodePath(String path) {
    String lower = path.toLowerCase(Locale.ROOT);
    return languages.stream().anyMatch(language -> lower.endsWith("." + language.fileExtension()));
  }

  /** A path token in prose ending in any known source-file extension. */
  public Pattern codePathPattern() {
    return codePath;
  }

  /** A fully-qualified code reference in prose, in any known language's spelling. */
  public Pattern qualifiedNamePattern() {
    return qualifiedName;
  }

  /** A bare identifier in prose that may name a code entity, in any known language's spelling. */
  public Pattern bareNamePattern() {
    return bareName;
  }

  /** Whether any language reads the bare name as compound (strong evidence). */
  public boolean isCompoundName(String bareName) {
    return languages.stream().anyMatch(language -> language.isCompoundName(bareName));
  }

  /**
   * Whether a referenced path or type name follows a known test convention — a test source root in
   * the path, or a simple name matching any language's test naming.
   */
  public boolean isTestReference(String reference) {
    if (reference.toLowerCase(Locale.ROOT).contains("/test/")) {
      return true;
    }
    String name = simpleName(reference);
    return languages.stream().anyMatch(language -> language.isTestName(name));
  }

  /** The simple type name: the last path/package segment, minus any known source extension. */
  private String simpleName(String reference) {
    String trimmed = reference;
    String lower = reference.toLowerCase(Locale.ROOT);
    for (SourceLanguage language : languages) {
      String suffix = "." + language.fileExtension();
      if (lower.endsWith(suffix)) {
        trimmed = reference.substring(0, reference.length() - suffix.length());
        break;
      }
    }
    int separator = Math.max(trimmed.lastIndexOf('/'), trimmed.lastIndexOf('.'));
    return trimmed.substring(separator + 1);
  }

  private static Pattern union(
      List<SourceLanguage> languages,
      java.util.function.Function<SourceLanguage, Pattern> pattern) {
    if (languages.isEmpty()) {
      return Pattern.compile(NOTHING);
    }
    return Pattern.compile(
        languages.stream()
            .map(language -> "(?:" + pattern.apply(language).pattern() + ")")
            .collect(Collectors.joining("|")));
  }
}
