package com.knowledgehub.system.domain;

import java.util.List;

/**
 * Immutable snapshot of the service's runtime information, surfaced over REST and MCP for operators
 * and AI agents.
 *
 * @param application the application name
 * @param version the build version, or {@code "unknown"} when no build metadata is present
 * @param activeProfiles the currently active Spring profiles
 * @param vectorStore the storage topology — always {@code neo4j+qdrant} (graph in Neo4j, vectors in
 *     Qdrant)
 */
public record SystemInfo(
    String application, String version, List<String> activeProfiles, String vectorStore) {}
