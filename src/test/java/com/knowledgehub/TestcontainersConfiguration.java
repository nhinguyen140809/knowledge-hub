package com.knowledgehub;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.Neo4jContainer;
import org.testcontainers.qdrant.QdrantContainer;
import org.testcontainers.utility.DockerImageName;

@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

  // This dev machine's Docker daemon has little memory to spare, so both containers get an
  // explicit hard cap instead of Docker's unbounded default.
  private static final long CONTAINER_MEMORY_BYTES = 512L * 1024 * 1024;

  @Bean
  @ServiceConnection
  Neo4jContainer<?> neo4jContainer() {
    return new Neo4jContainer<>(DockerImageName.parse("neo4j:5.26"))
        .withEnv("NEO4J_server_memory_heap_max__size", "256m")
        .withEnv("NEO4J_server_memory_pagecache_size", "64m")
        .withCreateContainerCmdModifier(
            cmd -> cmd.getHostConfig().withMemory(CONTAINER_MEMORY_BYTES));
  }

  @Bean
  @ServiceConnection
  QdrantContainer qdrantContainer() {
    return new QdrantContainer(DockerImageName.parse("qdrant/qdrant:v1.12.4"))
        .withCreateContainerCmdModifier(
            cmd -> cmd.getHostConfig().withMemory(CONTAINER_MEMORY_BYTES));
  }
}
