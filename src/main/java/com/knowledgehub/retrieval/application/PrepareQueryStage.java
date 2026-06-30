package com.knowledgehub.retrieval.application;

import com.knowledgehub.shared.pipeline.Stage;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

/**
 * Tokenizes the free-text query into keywords for the keyword path. It does not interpret intent -
 * tokenizing is just lower-casing, splitting on non-word characters, and dropping very short tokens
 * and stop words.
 *
 * <p>Embedding is deliberately not done here: it belongs to the semantic path, so an embedding
 * provider outage degrades only that path rather than failing the whole query.
 */
@Component
class PrepareQueryStage implements Stage<RetrievalContext> {

  private static final Pattern TOKEN = Pattern.compile("[A-Za-z0-9_]+");

  private static final Set<String> STOPWORDS =
      Set.of(
          "the", "a", "an", "of", "to", "in", "and", "or", "is", "are", "for", "on", "how", "what",
          "do", "does", "with", "that", "this", "it", "be", "as", "by", "at", "from");

  @Override
  public RetrievalContext apply(RetrievalContext context) {
    context.setKeywords(keywords(context.query().text()));
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
