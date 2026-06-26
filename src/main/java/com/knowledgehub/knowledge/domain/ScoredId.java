package com.knowledgehub.knowledge.domain;

/**
 * A chunk id with its relevance score, returned by the search ports. The application loads the full
 * node/metadata only after filtering, so search results stay lightweight.
 *
 * @param chunkId the matched chunk's stable id
 * @param score the relevance score (higher is more relevant)
 */
public record ScoredId(String chunkId, double score) {}
