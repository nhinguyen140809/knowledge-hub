package com.knowledgehub.knowledge.domain;

import java.util.regex.Pattern;

/**
 * A source language the knowledge context knows about — the single registration point for how that
 * language's files and code references are recognised. One implementation per language,
 * auto-collected into the {@code SourceLanguages} registry: supporting a new language starts with
 * one new {@code @Component} implementing this, with no change to existing classes. Today the
 * document linkers consume it (code-file test, reference spellings, test conventions);
 * language-specific parsers — a chunker, a structural extractor — attach alongside as their own
 * strategy implementations for the same language.
 */
public interface SourceLanguage {

  /** The language's source-file extension, lowercase and without the dot (e.g. {@code "java"}). */
  String fileExtension();

  /**
   * A fully-qualified code reference in prose — for Java, dotted lowercase packages ending in a
   * capitalised type like {@code com.example.Greeter}.
   */
  Pattern qualifiedNamePattern();

  /**
   * A bare identifier in prose that may name a code entity — for Java, a CamelCase type name like
   * {@code Greeter}.
   */
  Pattern bareNamePattern();

  /**
   * Whether a bare name is compound (multiple words joined, like {@code CodeChunker}) — strong
   * evidence, since compound identifiers rarely occur in prose by accident.
   */
  boolean isCompoundName(String bareName);

  /**
   * Whether a simple type or file name follows the language's test naming convention (for Java:
   * {@code Test}/{@code Tests}/{@code Spec}/{@code IT} suffixes).
   */
  boolean isTestName(String simpleName);
}
