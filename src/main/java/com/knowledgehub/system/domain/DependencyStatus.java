package com.knowledgehub.system.domain;

/** One dependency's reachability, keyed by its tracked name (e.g. neo4j, qdrant, embeddings). */
public record DependencyStatus(String name, DependencyState status) {}
