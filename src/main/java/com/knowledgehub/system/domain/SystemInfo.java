package com.knowledgehub.system.domain;

import java.util.List;

/**
 * Immutable snapshot of the service's runtime information, surfaced over REST and MCP for operators
 * and AI agents.
 *
 * @param productName the human-facing display name, operator-editable; falls back to the configured
 *     name and then to {@code application} when unset
 * @param application the technical application name
 * @param version the build version, or {@code "unknown"} when no build metadata is present
 * @param activeProfiles the currently active Spring profiles
 */
public record SystemInfo(
    String productName, String application, String version, List<String> activeProfiles) {}
