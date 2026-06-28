package com.knowledgehub.shared.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.knowledgehub.shared.config.AppProperties.Chunk;
import com.knowledgehub.shared.config.AppProperties.VectorStore;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class AppPropertiesTests {

  private static ValidatorFactory factory;
  private static Validator validator;

  @BeforeAll
  static void setUp() {
    factory = Validation.buildDefaultValidatorFactory();
    validator = factory.getValidator();
  }

  @AfterAll
  static void tearDown() {
    factory.close();
  }

  @Test
  void missingValuesFallBackToDefaultsAndPassValidation() {
    AppProperties properties = new AppProperties(null, null, null, null);

    assertThat(properties.vectorstore().mode()).isEqualTo("neo4j");
    assertThat(properties.retrieval().topK()).isEqualTo(10);
    assertThat(validator.validate(properties)).isEmpty();
  }

  @Test
  void unknownVectorStoreModeFailsValidation() {
    AppProperties properties = new AppProperties(new VectorStore("cassandra"), null, null, null);

    assertThat(validator.validate(properties)).isNotEmpty();
  }

  @Test
  void negativeChunkSizeFailsValidation() {
    AppProperties properties = new AppProperties(null, null, new Chunk(-1, 0), null);

    assertThat(validator.validate(properties)).isNotEmpty();
  }
}
