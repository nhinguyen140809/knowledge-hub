package com.knowledgehub.access.domain;

/**
 * System-wide reading policy for sources with no explicit grant.
 *
 * <ul>
 *   <li>{@code DENY} — a principal reads only the sources granted to it (or its groups).
 *   <li>{@code ALLOW} — a principal reads every source, except those that appear in some grant;
 *       such a source becomes restricted and is readable only by principals granted it.
 * </ul>
 */
public enum DefaultPolicy {
  DENY,
  ALLOW
}
