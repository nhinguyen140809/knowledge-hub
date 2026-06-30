package com.knowledgehub.knowledge.graph.infrastructure.extract;

import static org.assertj.core.api.Assertions.assertThat;

import com.knowledgehub.knowledge.graph.domain.EntityResolver;
import com.knowledgehub.knowledge.graph.domain.RelationType;
import com.knowledgehub.knowledge.graph.domain.Relationship;
import com.knowledgehub.knowledge.graph.domain.ResolutionScope;
import com.knowledgehub.knowledge.ingestion.domain.FsProvenance;
import com.knowledgehub.knowledge.ingestion.domain.RawArtifact;
import com.knowledgehub.knowledge.ingestion.infrastructure.MediaTypes;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class JavaStructuralExtractorTests {

  /** Resolves every qualified name to a deterministic id, so resolution is not under test here. */
  private final EntityResolver resolver =
      new EntityResolver() {
        @Override
        public Map<String, String> resolve(Collection<String> names, ResolutionScope scope) {
          Map<String, String> out = new HashMap<>();
          names.forEach(name -> out.put(name, "E:" + name));
          return out;
        }

        @Override
        public Map<String, List<String>> findByName(
            Collection<String> names, ResolutionScope scope) {
          return Map.of();
        }

        @Override
        public Map<String, List<String>> findByPath(
            Collection<String> paths, ResolutionScope scope) {
          return Map.of();
        }
      };

  private final JavaStructuralExtractor extractor = new JavaStructuralExtractor(resolver);

  private static RawArtifact java(String path, String text) {
    return RawArtifact.raw(
            path,
            MediaTypes.PLAIN_TEXT,
            text.getBytes(StandardCharsets.UTF_8),
            new FsProvenance("src", path, "hash", Instant.EPOCH))
        .withText(text);
  }

  private static List<Relationship> ofType(List<Relationship> rels, RelationType type) {
    return rels.stream().filter(r -> r.type() == type).toList();
  }

  @Test
  void extractsImportsExtendsAndSameTypeCalls() {
    String source =
        """
        package com.example;

        import com.other.Base;

        public class Child extends Base {
          void a() {
            b();
          }

          void b() {}
        }
        """;

    List<Relationship> rels = extractor.extract(java("Child.java", source));

    assertThat(ofType(rels, RelationType.IMPORTS))
        .singleElement()
        .satisfies(r -> assertThat(r.toId()).isEqualTo("E:com.other.Base"));
    assertThat(ofType(rels, RelationType.EXTENDS))
        .singleElement()
        .satisfies(r -> assertThat(r.toId()).isEqualTo("E:com.other.Base"));
    assertThat(ofType(rels, RelationType.CALLS)).hasSize(1);
    assertThat(ofType(rels, RelationType.IMPLEMENTS)).isEmpty();
    assertThat(rels).allSatisfy(r -> assertThat(r.confidence()).isEqualTo(1.0));
  }

  @Test
  void linksOverrideToTheSupertypeMethodItOverrides() {
    String source =
        """
        package com.example;

        import com.other.Base;

        public class Child extends Base {
          @Override
          public void run() {}
        }
        """;

    List<Relationship> rels = extractor.extract(java("Child.java", source));

    assertThat(ofType(rels, RelationType.OVERRIDES))
        .singleElement()
        .satisfies(
            r -> {
              assertThat(r.toId()).isEqualTo("E:com.other.Base#void run()");
              assertThat(r.confidence()).isEqualTo(1.0);
            });
  }

  @Test
  void resolvesSamePackageSupertypeFromANestedType() {
    String source =
        """
        package com.example;

        public class Outer {
          static class Inner extends Base {}
        }
        """;

    List<Relationship> rels = extractor.extract(java("Outer.java", source));

    // Base is unimported and same-package, so it resolves against the file's package, not Outer.
    assertThat(ofType(rels, RelationType.EXTENDS))
        .singleElement()
        .satisfies(r -> assertThat(r.toId()).isEqualTo("E:com.example.Base"));
  }

  @Test
  void emitsImplementsForEachInterface() {
    String source =
        """
        package com.example;

        public class Service implements Runnable, AutoCloseable {
          public void run() {}

          public void close() {}
        }
        """;

    List<Relationship> rels = extractor.extract(java("Service.java", source));

    assertThat(ofType(rels, RelationType.IMPLEMENTS)).hasSize(2);
  }

  @Test
  void doesNotLinkAmbiguousOverloadsOrQualifiedCalls() {
    String source =
        """
        package com.example;

        public class Calc {
          void run(java.util.List<String> items) {
            add(1);
            items.size();
          }

          void add(int x) {}

          void add(String x) {}
        }
        """;

    List<Relationship> rels = extractor.extract(java("Calc.java", source));

    // add(1) is ambiguous (two overloads) and items.size() is a qualified call — neither links.
    assertThat(ofType(rels, RelationType.CALLS)).isEmpty();
  }

  @Test
  void returnsNoRelationsForUnparseableSource() {
    assertThat(extractor.extract(java("Bad.java", "not java {{{"))).isEmpty();
  }
}
