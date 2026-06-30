package com.knowledgehub.retrieval.infrastructure.search;

import com.knowledgehub.knowledge.domain.Filter;
import com.knowledgehub.knowledge.domain.ScoredId;
import com.knowledgehub.retrieval.domain.KeywordSearchPort;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.stereotype.Component;

/**
 * BM25 keyword search over the Neo4j full-text indexes - {@code chunk_text} on chunk bodies and
 * {@code entity_name} on entity names/signatures. The two indexes are queried together and the best
 * score per id wins, so an exact identifier match surfaces whether it lives in prose or in a
 * symbol.
 *
 * <p>{@code allowedSources} is pushed into the query as a hard pre-filter (a disallowed source is
 * never scored); the functional {@code ref}/{@code type} filters are applied later against loaded
 * metadata.
 */
@Component
class Neo4jKeywordSearcher implements KeywordSearchPort {

  private static final String SEARCH =
      """
      CALL {
        CALL db.index.fulltext.queryNodes('chunk_text', $query) YIELD node, score
        WHERE $unrestricted OR node.source_id IN $allowedSources
        RETURN node.chunk_id AS id, score
        UNION
        CALL db.index.fulltext.queryNodes('entity_name', $query) YIELD node, score
        WHERE $unrestricted OR node.source_id IN $allowedSources
        RETURN node.entity_id AS id, score
      }
      WITH id, max(score) AS score
      RETURN id, score ORDER BY score DESC LIMIT $k
      """;

  /** Lucene query-syntax metacharacters; stripped so a raw identifier never breaks the parser. */
  private static final String LUCENE_SPECIALS = "[+\\-&|!(){}\\[\\]^\"~*?:\\\\/]";

  private final Neo4jClient client;

  Neo4jKeywordSearcher(Neo4jClient client) {
    this.client = client;
  }

  @Override
  public List<ScoredId> search(List<String> keywords, int k, Filter filter) {
    if (SourceFilters.restrictedToNothing(filter)) {
      return List.of();
    }
    String query = luceneQuery(keywords);
    if (query.isBlank()) {
      return List.of();
    }
    return client
        .query(SEARCH)
        .bindAll(
            Map.of(
                "query",
                query,
                "unrestricted",
                filter.isUnrestricted(),
                "allowedSources",
                SourceFilters.allowedSources(filter),
                "k",
                k))
        .fetchAs(ScoredId.class)
        .mappedBy((t, row) -> new ScoredId(row.get("id").asString(), row.get("score").asDouble()))
        .all()
        .stream()
        .toList();
  }

  /** Joins the keywords into a Lucene OR query, dropping metacharacters and blank tokens. */
  private static String luceneQuery(List<String> keywords) {
    return keywords.stream()
        .map(word -> word.replaceAll(LUCENE_SPECIALS, " ").trim().toLowerCase(Locale.ROOT))
        .filter(word -> !word.isBlank())
        .reduce((a, b) -> a + " " + b)
        .orElse("");
  }
}
