package com.knowledgehub.system.domain.port;

import com.knowledgehub.system.domain.GraphMetrics;

/**
 * Aggregate counts over the knowledge graph's own nodes (Source, File, Chunk, CodeEntity, Commit)
 * — deliberately excludes access-control (Principal, Credential) and system-metadata
 * (SystemConfig, SystemSettings) nodes, a different concern from "how big is the knowledge base".
 */
public interface KnowledgeGraphMetrics {

  /** A snapshot of the current counts. */
  GraphMetrics snapshot();
}
