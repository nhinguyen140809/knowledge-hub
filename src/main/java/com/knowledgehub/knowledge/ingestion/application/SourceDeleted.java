package com.knowledgehub.knowledge.ingestion.application;

/**
 * Domain event published after a source is removed, so the sync flow can evict its indexed
 * knowledge (vectors, nodes, edges) without {@code SourceService} depending on it. In-process only.
 *
 * @param sourceId the id of the removed source
 */
public record SourceDeleted(String sourceId) {}
