package com.knowledgehub.knowledge.indexing.application;

/**
 * Summary of one indexing run.
 *
 * @param sourceId the source that was indexed
 * @param filesRead artifacts successfully chunked and processed
 * @param filesSkipped artifacts skipped (no analyzer, or a failure isolated to that artifact)
 * @param chunksIndexed new/changed chunks embedded and stored
 * @param chunksCached chunks skipped because their content was already indexed
 * @param relationshipsLinked relationships written into the knowledge graph across all files
 */
public record IndexResult(
    String sourceId,
    int filesRead,
    int filesSkipped,
    int chunksIndexed,
    int chunksCached,
    int relationshipsLinked) {}
