package com.knowledgehub.knowledge.indexing.application;

/**
 * Summary of one indexing run.
 *
 * @param sourceId the source that was indexed
 * @param filesRead artifacts successfully chunked and processed
 * @param filesSkipped artifacts skipped (no chunker, or a failure isolated per NFR-6.1)
 * @param chunksIndexed new/changed chunks embedded and stored
 * @param chunksCached chunks skipped because their content was already indexed (FR-6.3)
 */
public record IndexResult(
    String sourceId, int filesRead, int filesSkipped, int chunksIndexed, int chunksCached) {}
