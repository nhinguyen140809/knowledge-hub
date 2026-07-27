package com.knowledgehub.system.domain;

/**
 * The knowledge base's scale — how much has been indexed. {@code documents} and {@code vectors}
 * should track each other closely; a large gap is itself a health signal (the embed step lagging
 * the graph).
 *
 * @param documents indexed chunk/document nodes
 * @param graphNodes total nodes across the knowledge graph's own labels
 * @param graphEdges total relationships among those nodes
 * @param vectors points in the vector store's collection
 */
public record KnowledgeStats(long documents, long graphNodes, long graphEdges, long vectors) {}
