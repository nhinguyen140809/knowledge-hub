package com.knowledgehub.shared.config;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** Defaulting and validation for the split {@code app.*} configuration records. */
class ConfigPropertiesTests {

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
  void everyRecordDefaultsAndPassesValidationWhenUnset() {
    EmbeddingProperties embedding = new EmbeddingProperties(null, null, null, null);
    ChunkProperties chunk = new ChunkProperties(null, null);
    CommitsProperties commits = new CommitsProperties(null);
    RetrievalProperties retrieval = new RetrievalProperties(null, null, null, null, null, null);
    LinkingProperties linking = new LinkingProperties(null);
    SecurityProperties security = new SecurityProperties(null, null, null);
    SourceProperties sources = new SourceProperties(null);

    // Assert a default *exists* (value present), not the exact number — so tuning a default
    // never breaks this test.
    assertThat(embedding.provider()).isNotBlank();
    assertThat(embedding.dimension()).isNotNull();
    assertThat(embedding.retryMaxAttempts()).isNotNull();
    assertThat(embedding.retryBackoffMs()).isNotNull();
    assertThat(chunk.maxTokens()).isNotNull();
    assertThat(commits.historyDepth()).isNotNull();
    assertThat(retrieval.topK()).isNotNull();
    assertThat(retrieval.weights().vector()).isNotNull();
    assertThat(retrieval.cacheTtl()).isNotNull();
    assertThat(linking.confidenceThreshold()).isNotNull();
    assertThat(security.credentialRetentionMonths()).isNotNull();
    assertThat(sources.staleAfter()).isNotNull();

    assertThat(validator.validate(embedding)).isEmpty();
    assertThat(validator.validate(chunk)).isEmpty();
    assertThat(validator.validate(commits)).isEmpty();
    assertThat(validator.validate(retrieval)).isEmpty();
    assertThat(validator.validate(linking)).isEmpty();
    assertThat(validator.validate(security)).isEmpty();
    assertThat(validator.validate(sources)).isEmpty();
  }

  @Test
  void unknownEmbeddingProviderFailsValidation() {
    assertThat(validator.validate(new EmbeddingProperties("anthropic", null, null, null)))
        .isNotEmpty();
  }

  @Test
  void negativeChunkSizeFailsValidation() {
    assertThat(validator.validate(new ChunkProperties(-1, 0))).isNotEmpty();
  }

  @Test
  void confidenceThresholdOutOfRangeFailsValidation() {
    assertThat(validator.validate(new LinkingProperties(1.5))).isNotEmpty();
  }
}
