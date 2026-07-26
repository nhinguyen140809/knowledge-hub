package com.knowledgehub.system.domain;

/** The Neo4j-derived portion of {@link KnowledgeStats}, before the vector count is added. */
public record GraphMetrics(long documents, long graphNodes, long graphEdges) {}
