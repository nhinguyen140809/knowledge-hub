package com.knowledgehub.system.domain;

import java.util.List;

/**
 * Immutable snapshot of the service's runtime information, surfaced over REST and MCP for operators
 * and AI agents.
 *
 * @param application the application name
 * @param version the build version, or {@code "unknown"} when no build metadata is present
 * @param activeProfiles the currently active Spring profiles
 */
public record SystemInfo(String application, String version, List<String> activeProfiles) {}
