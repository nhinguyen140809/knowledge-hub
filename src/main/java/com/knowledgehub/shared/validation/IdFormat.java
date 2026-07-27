package com.knowledgehub.shared.validation;

/**
 * The format shared by user-coined identifiers that end up in a URL path — a source id, a principal
 * id: lowercase letters, digits and single hyphens between them (kebab-case). Held in one place so
 * every request DTO that accepts such an id validates against exactly one rule and message instead
 * of drifting copies. Distinct from {@link com.knowledgehub.shared.id.IdFactory}, which derives
 * internal entity ids; this governs what a human may type.
 */
public final class IdFormat {

  /**
   * Kebab-case: lowercase alphanumerics in hyphen-separated groups, no leading or trailing hyphen.
   */
  public static final String PATTERN = "^[a-z0-9]+(-[a-z0-9]+)*$";

  /** Message for a {@code @Pattern(regexp = IdFormat.PATTERN)} violation. */
  public static final String MESSAGE = "must be lowercase letters, numbers and hyphens only";

  private IdFormat() {}
}
