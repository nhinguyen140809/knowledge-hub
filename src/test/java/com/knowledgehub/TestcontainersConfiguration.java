package com.knowledgehub;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.Neo4jContainer;
import org.testcontainers.qdrant.QdrantContainer;
import org.testcontainers.utility.DockerImageName;

@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

  @Bean
  @ServiceConnection
  Neo4jContainer<?> neo4jContainer() {
    return new Neo4jContainer<>(DockerImageName.parse("neo4j:5.26"));
  }

  @Bean
  @ServiceConnection
  QdrantContainer qdrantContainer() {
    return new QdrantContainer(DockerImageName.parse("qdrant/qdrant:v1.12.4"));
  }
}
