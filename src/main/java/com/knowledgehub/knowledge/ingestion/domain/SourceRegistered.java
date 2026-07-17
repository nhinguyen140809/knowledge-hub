package com.knowledgehub.knowledge.ingestion.domain;

/**
 * Domain event published after a source is registered, so the indexing flow can react (index it for
 * the first time) without {@code SourceService} calling indexing directly. In-process only.
 *
 * @param sourceId the id of the newly registered source
 */
public record SourceRegistered(String sourceId) {}
