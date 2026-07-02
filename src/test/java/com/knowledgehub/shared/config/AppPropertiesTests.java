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
  void everyTunableGetsADefaultAndPassesValidation() {
    AppProperties properties = new AppProperties(null, null, null, null, null);

    // Assert a default *exists* (value present), not the exact number — so tuning a default
    // never breaks this test.
    assertThat(properties.embedding().provider()).isNotBlank();
    assertThat(properties.embedding().dimension()).isNotNull();
    assertThat(properties.embedding().retryMaxAttempts()).isNotNull();
    assertThat(properties.embedding().retryBackoffMs()).isNotNull();
    assertThat(properties.chunk().maxTokens()).isNotNull();
    assertThat(properties.retrieval().topK()).isNotNull();
    assertThat(properties.retrieval().weights().vector()).isNotNull();
    assertThat(properties.retrieval().cacheTtl()).isNotNull();
    assertThat(properties.linking().confidenceThreshold()).isNotNull();
    assertThat(properties.security().credentialRetentionMonths()).isNotNull();
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
