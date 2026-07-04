package com.knowledgehub.knowledge.infrastructure.lang;

import com.knowledgehub.knowledge.domain.SourceLanguage;
import java.util.List;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

/**
 * Java's reference spellings: dotted lowercase packages ending in a capitalised type for qualified
 * names, CamelCase for bare type names, an inner lowercase-to-uppercase step marking a compound,
 * and the conventional test suffixes.
 */
@Component
public class JavaLanguage implements SourceLanguage {

  /** A qualified reference like {@code com.example.Greeter} (lowercase packages, capital type). */
  private static final Pattern QUALIFIED =
      Pattern.compile("\\b(?:[a-z][\\w]*\\.)+[A-Z][A-Za-z0-9]*\\b");

  /** A bare CamelCase type name like {@code Greeter} (capitalised, contains a lowercase letter). */
  private static final Pattern CAMEL_CASE =
      Pattern.compile("\\b[A-Z][A-Za-z0-9]*[a-z][A-Za-z0-9]*\\b");

  /** A second capitalised segment (e.g. the {@code C} in {@code CodeChunker}) marks a compound. */
  private static final Pattern COMPOUND = Pattern.compile("[a-z0-9][A-Z]");

  private static final List<String> TEST_SUFFIXES = List.of("Test", "Tests", "Spec", "IT");

  @Override
  public String fileExtension() {
    return "java";
  }

  @Override
  public Pattern qualifiedNamePattern() {
    return QUALIFIED;
  }

  @Override
  public Pattern bareNamePattern() {
    return CAMEL_CASE;
  }

  @Override
  public boolean isCompoundName(String bareName) {
    return COMPOUND.matcher(bareName).find();
  }

  @Override
  public boolean isTestName(String simpleName) {
    return TEST_SUFFIXES.stream().anyMatch(simpleName::endsWith);
  }
}
