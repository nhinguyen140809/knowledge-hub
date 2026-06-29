package com.knowledgehub.knowledge.ingestion.infrastructure.persistence;

import org.springframework.data.neo4j.repository.Neo4jRepository;

/** Spring Data Neo4j repository for {@link SourceNode}; used only by {@link Neo4jSourceAdapter}. */
interface SourceNodeRepository extends Neo4jRepository<SourceNode, String> {}
