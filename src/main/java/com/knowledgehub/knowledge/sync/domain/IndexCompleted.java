package com.knowledgehub.knowledge.sync.domain;

/**
 * Domain event published after a sync changes a source's index, so dependents can react - retrieval
 * drops its now-stale cached results. In-process only.
 *
 * @param sourceId the source whose index changed
 */
public record IndexCompleted(String sourceId) {}
