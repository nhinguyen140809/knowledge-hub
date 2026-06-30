package com.knowledgehub.shared.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.knowledgehub.shared.config.AppProperties.Chunk;
import com.knowledgehub.shared.config.AppProperties.Embedding;
import com.knowledgehub.shared.config.AppProperties.Linking;
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
    AppProperties properties = new AppProperties(null, null, null, null, null);

    assertThat(properties.embedding().provider()).isEqualTo("api");
    assertThat(properties.embedding().dimension()).isEqualTo(1536);
    assertThat(properties.retrieval().topK()).isEqualTo(10);
    assertThat(properties.linking().confidenceThreshold()).isEqualTo(0.5);
    assertThat(validator.validate(properties)).isEmpty();
  }

  @Test
  void unknownEmbeddingProviderFailsValidation() {
    AppProperties properties =
        new AppProperties(new Embedding("anthropic", null, null, null), null, null, null, null);

    assertThat(validator.validate(properties)).isNotEmpty();
  }

  @Test
  void negativeChunkSizeFailsValidation() {
    AppProperties properties = new AppProperties(null, new Chunk(-1, 0), null, null, null);

    assertThat(validator.validate(properties)).isNotEmpty();
  }

  @Test
  void confidenceThresholdOutOfRangeFailsValidation() {
    AppProperties properties = new AppProperties(null, null, null, new Linking(1.5), null);

    assertThat(validator.validate(properties)).isNotEmpty();
  }
}
