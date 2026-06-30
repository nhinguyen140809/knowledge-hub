package com.knowledgehub.retrieval.application;

import com.knowledgehub.knowledge.domain.EmbeddingPort;
import com.knowledgehub.shared.pipeline.Stage;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

/**
 * Turns the free-text query into the two inputs the search paths need: one embedding (for semantic
 * search) and a list of keywords (for BM25). It does not interpret intent - tokenizing is just
 * lower-casing, splitting on non-word characters, and dropping very short tokens and stop words.
 */
@Component
class PrepareQueryStage implements Stage<RetrievalContext> {

  private static final Pattern TOKEN = Pattern.compile("[A-Za-z0-9_]+");

  private static final Set<String> STOPWORDS =
      Set.of(
          "the", "a", "an", "of", "to", "in", "and", "or", "is", "are", "for", "on", "how", "what",
          "do", "does", "with", "that", "this", "it", "be", "as", "by", "at", "from");

  private final EmbeddingPort embeddingPort;

  PrepareQueryStage(EmbeddingPort embeddingPort) {
    this.embeddingPort = embeddingPort;
  }

  @Override
  public RetrievalContext apply(RetrievalContext context) {
    String text = context.query().text();
    context.setEmbedding(embeddingPort.embed(text));
    context.setKeywords(keywords(text));
    return context;
  }

  /** Lower-cased word tokens, minus stop words and one-character tokens, de-duplicated in order. */
  private static List<String> keywords(String text) {
    Set<String> keywords = new LinkedHashSet<>();
    Matcher matcher = TOKEN.matcher(text.toLowerCase(Locale.ROOT));
    while (matcher.find()) {
      String token = matcher.group();
      if (token.length() > 1 && !STOPWORDS.contains(token)) {
        keywords.add(token);
      }
    }
    return List.copyOf(keywords);
  }
}
