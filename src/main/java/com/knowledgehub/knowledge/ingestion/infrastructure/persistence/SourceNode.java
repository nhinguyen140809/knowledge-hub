package com.knowledgehub.knowledge.ingestion.infrastructure.persistence;

import java.util.List;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Property;

/**
 * Spring Data Neo4j persistence entity for a {@code :Source} node. Kept separate from the domain
 * {@code Source} so framework annotations stay out of the domain; {@link Neo4jSourceAdapter} maps
 * between the two. Property names match the schema constraints (snake_case).
 */
@Node("Source")
class SourceNode {

  @Id
  @Property("source_id")
  private final String sourceId;

  @Property("type")
  private final String type;

  @Property("uri_or_path")
  private final String uriOrPath;

  @Property("ref")
  private final String ref;

  @Property("include")
  private final List<String> include;

  @Property("ignore")
  private final List<String> ignore;

  SourceNode(
      String sourceId,
      String type,
      String uriOrPath,
      String ref,
      List<String> include,
      List<String> ignore) {
    this.sourceId = sourceId;
    this.type = type;
    this.uriOrPath = uriOrPath;
    this.ref = ref;
    this.include = include;
    this.ignore = ignore;
  }

  String sourceId() {
    return sourceId;
  }

  String type() {
    return type;
  }

  String uriOrPath() {
    return uriOrPath;
  }

  String ref() {
    return ref;
  }

  List<String> include() {
    return include;
  }

  List<String> ignore() {
    return ignore;
  }
}
