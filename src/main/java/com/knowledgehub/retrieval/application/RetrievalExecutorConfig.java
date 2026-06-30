package com.knowledgehub.retrieval.application;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * The executor the retrieval pipeline fans its search paths out on. Virtual threads suit the work -
 * each path is a blocking call to Qdrant or Neo4j, so the threads spend their time waiting, and a
 * thread-per-task model keeps the wiring trivial without sizing a pool.
 */
@Configuration
class RetrievalExecutorConfig {

  @Bean(destroyMethod = "close")
  ExecutorService retrievalExecutor() {
    return Executors.newVirtualThreadPerTaskExecutor();
  }
}
