package com.knowledgehub.knowledge.analysis.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import com.knowledgehub.knowledge.analysis.domain.AnalysisResult;
import com.knowledgehub.knowledge.analysis.domain.ChunkConfig;
import com.knowledgehub.knowledge.analysis.domain.CodeEntity;
import com.knowledgehub.knowledge.analysis.domain.PendingReference;
import com.knowledgehub.knowledge.domain.RelationType;
import com.knowledgehub.knowledge.domain.Relationship;
import com.knowledgehub.knowledge.infrastructure.lang.JavaLanguage;
import com.knowledgehub.knowledge.ingestion.domain.FsProvenance;
import com.knowledgehub.knowledge.ingestion.domain.RawArtifact;
import com.knowledgehub.knowledge.ingestion.infrastructure.MediaTypes;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * The relation half of {@link JavaAnalyzer}: same-file edges come out resolved, everything else as
 * pending references carrying the fully-qualified target name for the linking step to resolve.
 */
class JavaAnalyzerRelationTests {

  private final JavaAnalyzer analyzer = new JavaAnalyzer(new JavaLanguage());

  private static RawArtifact java(String path, String text) {
    return RawArtifact.raw(
            path,
            MediaTypes.PLAIN_TEXT,
            text.getBytes(StandardCharsets.UTF_8),
            new FsProvenance("src", path, "hash", Instant.EPOCH))
        .withText(text);
  }

  private AnalysisResult analyze(String path, String source) {
    return analyzer.analyze(java(path, source), new ChunkConfig(512, 0));
  }

  private static List<PendingReference> ofType(List<PendingReference> refs, RelationType type) {
    return refs.stream().filter(r -> r.relationType() == type).toList();
  }

  private static List<Relationship> relationsOfType(List<Relationship> rels, RelationType type) {
    return rels.stream().filter(r -> r.type() == type).toList();
  }

  @Test
  void emitsImportsExtendsAsPendingAndSameTypeCallsAsResolved() {
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

    AnalysisResult result = analyze("Child.java", source);

    assertThat(ofType(result.pendingReferences(), RelationType.IMPORTS))
        .singleElement()
        .satisfies(r -> assertThat(r.targetName()).isEqualTo("com.other.Base"));
    assertThat(ofType(result.pendingReferences(), RelationType.EXTENDS))
        .singleElement()
        .satisfies(r -> assertThat(r.targetName()).isEqualTo("com.other.Base"));
    assertThat(ofType(result.pendingReferences(), RelationType.IMPLEMENTS)).isEmpty();
    assertThat(relationsOfType(result.relations(), RelationType.CALLS)).hasSize(1);
    assertThat(result.relations()).allSatisfy(r -> assertThat(r.confidence()).isEqualTo(1.0));
  }

  @Test
  void pendingOverrideTargetsTheSupertypeMethodSignature() {
    String source =
        """
        package com.example;

        import com.other.Base;

        public class Child extends Base {
          @Override
          public void run() {}
        }
        """;

    AnalysisResult result = analyze("Child.java", source);

    assertThat(ofType(result.pendingReferences(), RelationType.OVERRIDES))
        .singleElement()
        .satisfies(r -> assertThat(r.targetName()).isEqualTo("com.other.Base#void run()"));
  }

  @Test
  void qualifiesSamePackageSupertypeFromANestedType() {
    String source =
        """
        package com.example;

        public class Outer {
          static class Inner extends Base {}
        }
        """;

    AnalysisResult result = analyze("Outer.java", source);

    // Base is unimported and same-package, so it resolves against the file's package, not Outer.
    assertThat(ofType(result.pendingReferences(), RelationType.EXTENDS))
        .singleElement()
        .satisfies(r -> assertThat(r.targetName()).isEqualTo("com.example.Base"));
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

    AnalysisResult result = analyze("Service.java", source);

    assertThat(ofType(result.pendingReferences(), RelationType.IMPLEMENTS)).hasSize(2);
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

    AnalysisResult result = analyze("Calc.java", source);

    // add(1) is ambiguous (two overloads) and items.size() is a qualified call — neither links.
    assertThat(relationsOfType(result.relations(), RelationType.CALLS)).isEmpty();
  }

  @Test
  void emitsDeepReferencesFromDeclarationsAndBodies() {
    String source =
        """
        package com.example;

        import com.other.Boom;
        import com.other.Helper;
        import com.other.Marker;

        public class Service {
          Helper helper = new Helper();

          @Marker
          void run() throws Boom {
            Helper.stat();
          }
        }
        """;

    AnalysisResult result = analyze("Service.java", source);
    List<PendingReference> refs = result.pendingReferences();

    String fieldId = CodeEntity.deriveId("src", "Service.java", "com.example.Service#helper");
    assertThat(ofType(refs, RelationType.HAS_TYPE))
        .singleElement()
        .satisfies(
            r -> {
              assertThat(r.fromId()).isEqualTo(fieldId);
              assertThat(r.targetName()).isEqualTo("com.other.Helper");
            });
    assertThat(ofType(refs, RelationType.INSTANTIATES))
        .singleElement()
        .satisfies(r -> assertThat(r.targetName()).isEqualTo("com.other.Helper"));
    assertThat(ofType(refs, RelationType.ANNOTATED_WITH))
        .singleElement()
        .satisfies(r -> assertThat(r.targetName()).isEqualTo("com.other.Marker"));
    assertThat(ofType(refs, RelationType.THROWS))
        .singleElement()
        .satisfies(r -> assertThat(r.targetName()).isEqualTo("com.other.Boom"));
    assertThat(ofType(refs, RelationType.REFERENCES))
        .singleElement()
        .satisfies(r -> assertThat(r.targetName()).isEqualTo("com.other.Helper"));
  }

  @Test
  void emitsSameTypeFieldReadsAndWritesButSkipsShadowedNames() {
    String source =
        """
        package com.example;

        public class Counter {
          int count;
          int limit;

          void bump() {
            count++;
            this.count += 2;
          }

          int remaining(int count) {
            return limit - count;
          }
        }
        """;

    AnalysisResult result = analyze("Counter.java", source);
    List<Relationship> rels = result.relations();

    String countId = CodeEntity.deriveId("src", "Counter.java", "com.example.Counter#count");
    String limitId = CodeEntity.deriveId("src", "Counter.java", "com.example.Counter#limit");
    String bumpId = CodeEntity.deriveId("src", "Counter.java", "com.example.Counter#void bump()");

    assertThat(relationsOfType(rels, RelationType.WRITES))
        .singleElement()
        .satisfies(
            r -> {
              assertThat(r.fromId()).isEqualTo(bumpId);
              assertThat(r.toId()).isEqualTo(countId);
            });
    // bump reads count (++ and += read the old value); remaining reads limit, but its
    // count parameter shadows the field, so no read of count is recorded there.
    assertThat(relationsOfType(rels, RelationType.READS))
        .extracting(Relationship::toId)
        .containsExactlyInAnyOrder(countId, limitId);
  }

  @Test
  void pendingReferenceFromIdsMatchTheStoredEntityIds() {
    String source =
        """
        package com.example;

        import com.other.Base;

        public class Child extends Base {}
        """;

    AnalysisResult result = analyze("Child.java", source);

    // Every from-end must be an entity the same pass stored — the id derivations are shared.
    List<String> entityIds = result.codeEntities().stream().map(CodeEntity::entityId).toList();
    assertThat(result.pendingReferences())
        .allSatisfy(r -> assertThat(entityIds).contains(r.fromId()));
  }
}
