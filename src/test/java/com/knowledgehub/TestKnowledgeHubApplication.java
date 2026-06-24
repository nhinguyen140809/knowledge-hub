package com.knowledgehub;

import org.springframework.boot.SpringApplication;

public class TestKnowledgeHubApplication {

  public static void main(String[] args) {
    SpringApplication.from(KnowledgeHubApplication::main)
        .with(TestcontainersConfiguration.class)
        .run(args);
  }
}
