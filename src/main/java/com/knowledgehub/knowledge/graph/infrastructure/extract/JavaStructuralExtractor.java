package com.knowledgehub.knowledge.graph.infrastructure.extract;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.CallableDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.EnumDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.expr.ThisExpr;
import com.github.javaparser.ast.expr.UnaryExpr;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.ReferenceType;
import com.knowledgehub.knowledge.graph.domain.EntityResolver;
import com.knowledgehub.knowledge.graph.domain.RelationType;
import com.knowledgehub.knowledge.graph.domain.Relationship;
import com.knowledgehub.knowledge.graph.domain.ResolutionScope;
import com.knowledgehub.knowledge.graph.domain.StructuralExtractor;
import com.knowledgehub.knowledge.indexing.domain.CodeEntity;
import com.knowledgehub.knowledge.ingestion.domain.RawArtifact;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Reads structural and deep relationships out of Java source via JavaParser. The structural pass
 * emits {@code IMPORTS} (file imports resolved to entities), {@code EXTENDS}/{@code IMPLEMENTS}
 * (inheritance), {@code OVERRIDES} (an {@code @Override} method to the same-signature method it
 * overrides in a supertype), and {@code CALLS} for calls to a sibling method of the same type. The
 * deep pass emits what single-file syntax still decides with certainty: {@code INSTANTIATES}
 * ({@code new Foo()}), {@code THROWS} (declared exceptions), {@code ANNOTATED_WITH} (annotations on
 * types and callables), {@code HAS_TYPE} (a field's declared type), {@code REFERENCES}
 * (static-scope use of an imported type), and same-type {@code READS}/{@code WRITES} of fields. All
 * edges are deterministic (confidence 1); a reference that does not resolve to an indexed entity is
 * dropped rather than guessed.
 *
 * <p>Targets are resolved through the {@link EntityResolver}, so an import, supertype or overridden
 * method that lives in another source links across source boundaries. Relations that need full type
 * inference (calls or accesses on other objects) are out of scope here and belong to a
 * symbol-solver-backed extractor.
 *
 * <p>Example — extracting this file:
 *
 * <pre>{@code
 * package com.example;
 *
 * import com.other.Base;
 * import com.other.Clock;
 *
 * public class Greeter extends Base {
 *   private final Clock clock = new Clock();
 *
 *   @Override
 *   public String greet() throws GreetingException {
 *     return stamp();
 *   }
 *
 *   private String stamp() {
 *     return Clock.format(clock.now());
 *   }
 * }
 * }</pre>
 *
 * yields, when the targets are indexed: {@code Greeter IMPORTS Base, Clock}; {@code Greeter EXTENDS
 * Base}; {@code greet() OVERRIDES Base#greet()}; {@code greet() CALLS stamp()}; field {@code clock
 * HAS_TYPE Clock} and {@code INSTANTIATES Clock} (from its initializer); {@code greet() THROWS
 * com.example.GreetingException} (unimported, so resolved against the file's own package); {@code
 * stamp() REFERENCES Clock} (static-scope call); {@code stamp() READS clock}. Nothing is emitted
 * for {@code clock.now()} — a call on another object needs type inference — and {@code @Override}
 * links nowhere because {@code java.lang.Override} is not an indexed entity.
 */
@Component
public class JavaStructuralExtractor implements StructuralExtractor {

  private static final Logger log = LoggerFactory.getLogger(JavaStructuralExtractor.class);

  private final EntityResolver resolver;

  public JavaStructuralExtractor(EntityResolver resolver) {
    this.resolver = resolver;
  }

  @Override
  public boolean supports(RawArtifact artifact) {
    return artifact.text() != null && artifact.path().toLowerCase(Locale.ROOT).endsWith(".java");
  }

  /**
   * Parses the artifact's text and walks every top-level type. Edges whose both ends live in this
   * file (same-type calls, field reads/writes) are emitted directly — their entity ids are derived
   * locally from the identity parts. Every other reference is collected as a pending (from,
   * target-name, edge-type) triple and resolved in one batched lookup at the end; a name the
   * resolver cannot settle is dropped. Unparseable source yields an empty list rather than failing
   * the pipeline.
   */
  @Override
  public List<Relationship> extract(RawArtifact artifact) {
    ParseResult<CompilationUnit> parsed = new JavaParser().parse(artifact.text());
    if (!parsed.isSuccessful() || parsed.getResult().isEmpty()) {
      log.debug("Skipping structural extraction for unparseable {}", artifact.path());
      return List.of();
    }
    CompilationUnit cu = parsed.getResult().get();
    String sourceId = artifact.provenance().sourceId();
    String path = artifact.path();
    String packageName = cu.getPackageDeclaration().map(p -> p.getNameAsString()).orElse("");
    Map<String, String> importsByName = importsByName(cu);

    // Same-type calls resolve locally; imports, inheritance and overrides are gathered then
    // resolved in one batch, so a file is one resolver round-trip rather than one per reference.
    List<Relationship> out = new ArrayList<>();
    List<PendingRef> refs = new ArrayList<>();
    for (TypeDeclaration<?> type : cu.getTypes()) {
      String topId =
          CodeEntity.deriveId(sourceId, path, qualified(packageName, type.getNameAsString()));
      collectImports(cu, topId, refs);
      collectType(type, packageName, packageName, sourceId, path, importsByName, refs, out);
    }
    resolveRefs(refs, new ResolutionScope(sourceId), out);
    return out;
  }

  /** Resolves the gathered references in one round-trip and appends the edges that resolve. */
  private void resolveRefs(List<PendingRef> refs, ResolutionScope scope, List<Relationship> out) {
    Set<String> fqns = refs.stream().map(PendingRef::fqn).collect(Collectors.toSet());
    Map<String, String> resolved = resolver.resolve(fqns, scope);
    for (PendingRef ref : refs) {
      String toId = resolved.get(ref.fqn());
      if (toId != null) {
        out.add(Relationship.deterministic(ref.fromId(), toId, ref.type()));
      }
    }
  }

  /** Simple type name -> fully-qualified name, from the file's single-type imports. */
  private static Map<String, String> importsByName(CompilationUnit cu) {
    Map<String, String> imports = new HashMap<>();
    for (ImportDeclaration imp : cu.getImports()) {
      if (!imp.isAsterisk() && !imp.isStatic()) {
        String fqn = imp.getNameAsString();
        imports.put(fqn.substring(fqn.lastIndexOf('.') + 1), fqn);
      }
    }
    return imports;
  }

  /** A file's imports become IMPORTS references from each top-level type to the imported entity. */
  private static void collectImports(CompilationUnit cu, String fromId, List<PendingRef> refs) {
    for (ImportDeclaration imp : cu.getImports()) {
      if (!imp.isAsterisk() && !imp.isStatic()) {
        refs.add(new PendingRef(fromId, imp.getNameAsString(), RelationType.IMPORTS));
      }
    }
  }

  /**
   * One type's full harvest — inheritance, overrides, annotations, per-callable deep references,
   * field types, same-type calls and field accesses — then recursion into its nested types. Local
   * edges go straight to {@code out}; cross-file references accumulate in {@code refs}.
   */
  private void collectType(
      TypeDeclaration<?> type,
      String enclosingQualifier,
      String packageName,
      String sourceId,
      String path,
      Map<String, String> importsByName,
      List<PendingRef> refs,
      List<Relationship> out) {
    String qualifiedName = qualified(enclosingQualifier, type.getNameAsString());
    String typeId = CodeEntity.deriveId(sourceId, path, qualifiedName);

    List<String> supertypes = collectInheritance(type, typeId, packageName, importsByName, refs);
    collectOverrides(type, sourceId, path, qualifiedName, supertypes, refs);
    collectAnnotations(type.getAnnotations(), typeId, packageName, importsByName, refs);
    collectCallableRefs(type, sourceId, path, qualifiedName, packageName, importsByName, refs);
    collectFieldRefs(type, sourceId, path, qualifiedName, packageName, importsByName, refs);
    emitSameTypeCalls(type, sourceId, path, qualifiedName, out);
    emitFieldAccess(type, sourceId, path, qualifiedName, out);

    for (Node member : type.getMembers()) {
      if (member instanceof TypeDeclaration<?> nested) {
        collectType(nested, qualifiedName, packageName, sourceId, path, importsByName, refs, out);
      }
    }
  }

  /** Emits the type's EXTENDS/IMPLEMENTS edges and returns every supertype's qualified name. */
  private static List<String> collectInheritance(
      TypeDeclaration<?> type,
      String typeId,
      String packageName,
      Map<String, String> importsByName,
      List<PendingRef> refs) {
    List<String> supertypes = new ArrayList<>();
    if (type instanceof ClassOrInterfaceDeclaration cls) {
      for (ClassOrInterfaceType ext : cls.getExtendedTypes()) {
        String fqn = fqnOf(ext, packageName, importsByName);
        supertypes.add(fqn);
        refs.add(new PendingRef(typeId, fqn, RelationType.EXTENDS));
      }
      for (ClassOrInterfaceType impl : cls.getImplementedTypes()) {
        String fqn = fqnOf(impl, packageName, importsByName);
        supertypes.add(fqn);
        refs.add(new PendingRef(typeId, fqn, RelationType.IMPLEMENTS));
      }
    } else if (type instanceof EnumDeclaration en) {
      for (ClassOrInterfaceType impl : en.getImplementedTypes()) {
        String fqn = fqnOf(impl, packageName, importsByName);
        supertypes.add(fqn);
        refs.add(new PendingRef(typeId, fqn, RelationType.IMPLEMENTS));
      }
    }
    return supertypes;
  }

  /**
   * A method carrying {@code @Override} overrides the same-signature method in a supertype; emit an
   * OVERRIDES edge to whichever supertype actually declares it (others simply do not resolve).
   */
  private static void collectOverrides(
      TypeDeclaration<?> type,
      String sourceId,
      String path,
      String qualifiedName,
      List<String> supertypes,
      List<PendingRef> refs) {
    if (supertypes.isEmpty()) {
      return;
    }
    for (MethodDeclaration method : type.getMethods()) {
      if (method.getAnnotationByName("Override").isEmpty()) {
        continue;
      }
      String fromId = callableEntityId(sourceId, path, qualifiedName, method);
      String signature = "#" + method.getDeclarationAsString(false, false, false);
      for (String supertype : supertypes) {
        refs.add(new PendingRef(fromId, supertype + signature, RelationType.OVERRIDES));
      }
    }
  }

  /** Annotations on a declaration become ANNOTATED_WITH references from its entity. */
  private static void collectAnnotations(
      List<AnnotationExpr> annotations,
      String fromId,
      String packageName,
      Map<String, String> importsByName,
      List<PendingRef> refs) {
    for (AnnotationExpr annotation : annotations) {
      String fqn = fqnOfName(annotation.getNameAsString(), packageName, importsByName);
      refs.add(new PendingRef(fromId, fqn, RelationType.ANNOTATED_WITH));
    }
  }

  /**
   * Per-callable deep references: annotations, declared throws, instantiations, and static-scope
   * uses of an imported type. Gathered as pending references and resolved in the same batch as the
   * structural ones.
   */
  private static void collectCallableRefs(
      TypeDeclaration<?> type,
      String sourceId,
      String path,
      String qualifiedName,
      String packageName,
      Map<String, String> importsByName,
      List<PendingRef> refs) {
    for (CallableDeclaration<?> callable : callables(type)) {
      String fromId = callableEntityId(sourceId, path, qualifiedName, callable);
      collectAnnotations(callable.getAnnotations(), fromId, packageName, importsByName, refs);
      for (ReferenceType thrown : callable.getThrownExceptions()) {
        if (thrown.isClassOrInterfaceType()) {
          String fqn = fqnOf(thrown.asClassOrInterfaceType(), packageName, importsByName);
          refs.add(new PendingRef(fromId, fqn, RelationType.THROWS));
        }
      }
      Set<String> seen = new LinkedHashSet<>();
      for (ObjectCreationExpr creation : callable.findAll(ObjectCreationExpr.class)) {
        String fqn = fqnOf(creation.getType(), packageName, importsByName);
        if (seen.add(fqn)) {
          refs.add(new PendingRef(fromId, fqn, RelationType.INSTANTIATES));
        }
      }
      for (String fqn : staticScopeReferences(callable, importsByName)) {
        if (seen.add(fqn)) {
          refs.add(new PendingRef(fromId, fqn, RelationType.REFERENCES));
        }
      }
    }
  }

  /**
   * Types referenced by name as a scope ({@code Foo.bar()}, {@code Foo.CONSTANT}) where the name is
   * an explicit import — the one case a scope name is certainly a type and not a variable.
   */
  private static Set<String> staticScopeReferences(
      CallableDeclaration<?> callable, Map<String, String> importsByName) {
    Set<String> fqns = new LinkedHashSet<>();
    for (MethodCallExpr call : callable.findAll(MethodCallExpr.class)) {
      call.getScope().ifPresent(scope -> addScopeReference(scope, importsByName, fqns));
    }
    for (FieldAccessExpr access : callable.findAll(FieldAccessExpr.class)) {
      addScopeReference(access.getScope(), importsByName, fqns);
    }
    return fqns;
  }

  private static void addScopeReference(
      Expression scope, Map<String, String> importsByName, Set<String> fqns) {
    if (scope instanceof NameExpr name && importsByName.containsKey(name.getNameAsString())) {
      fqns.add(importsByName.get(name.getNameAsString()));
    }
  }

  /**
   * A field's declared type becomes HAS_TYPE and its initializer's instantiations become
   * INSTANTIATES, both from the field's own entity.
   */
  private static void collectFieldRefs(
      TypeDeclaration<?> type,
      String sourceId,
      String path,
      String qualifiedName,
      String packageName,
      Map<String, String> importsByName,
      List<PendingRef> refs) {
    for (FieldDeclaration field : type.getFields()) {
      for (VariableDeclarator variable : field.getVariables()) {
        String fieldId = fieldEntityId(sourceId, path, qualifiedName, variable.getNameAsString());
        if (variable.getType().isClassOrInterfaceType()) {
          String fqn =
              fqnOf(variable.getType().asClassOrInterfaceType(), packageName, importsByName);
          refs.add(new PendingRef(fieldId, fqn, RelationType.HAS_TYPE));
        }
        for (ObjectCreationExpr creation :
            variable
                .getInitializer()
                .map(i -> i.findAll(ObjectCreationExpr.class))
                .orElse(List.of())) {
          String fqn = fqnOf(creation.getType(), packageName, importsByName);
          refs.add(new PendingRef(fieldId, fqn, RelationType.INSTANTIATES));
        }
      }
    }
  }

  /**
   * Same-type field reads and writes, resolved locally like same-type calls: a {@code this.x}
   * access, or a bare name that no parameter or local anywhere in the callable shadows — dropping a
   * real access is acceptable, recording a false one is not.
   */
  private static void emitFieldAccess(
      TypeDeclaration<?> type,
      String sourceId,
      String path,
      String qualifiedName,
      List<Relationship> out) {
    Map<String, String> fieldIds = new HashMap<>();
    for (FieldDeclaration field : type.getFields()) {
      for (VariableDeclarator variable : field.getVariables()) {
        fieldIds.put(
            variable.getNameAsString(),
            fieldEntityId(sourceId, path, qualifiedName, variable.getNameAsString()));
      }
    }
    if (fieldIds.isEmpty()) {
      return;
    }
    for (CallableDeclaration<?> callable : callables(type)) {
      String fromId = callableEntityId(sourceId, path, qualifiedName, callable);
      Set<String> shadowed = shadowedNames(callable);
      Set<Expression> writeTargets = Collections.newSetFromMap(new IdentityHashMap<>());
      Set<String> written = new LinkedHashSet<>();
      Set<String> read = new LinkedHashSet<>();
      for (AssignExpr assign : callable.findAll(AssignExpr.class)) {
        String name = accessedField(assign.getTarget(), fieldIds, shadowed);
        if (name != null) {
          writeTargets.add(assign.getTarget());
          written.add(name);
          if (assign.getOperator() != AssignExpr.Operator.ASSIGN) {
            read.add(name); // a compound assignment reads the old value too
          }
        }
      }
      for (UnaryExpr unary : callable.findAll(UnaryExpr.class)) {
        if (incrementsOrDecrements(unary)) {
          String name = accessedField(unary.getExpression(), fieldIds, shadowed);
          if (name != null) {
            writeTargets.add(unary.getExpression());
            written.add(name);
            read.add(name);
          }
        }
      }
      for (NameExpr name : callable.findAll(NameExpr.class)) {
        if (!writeTargets.contains(name)) {
          String field = accessedField(name, fieldIds, shadowed);
          if (field != null) {
            read.add(field);
          }
        }
      }
      for (FieldAccessExpr access : callable.findAll(FieldAccessExpr.class)) {
        if (!writeTargets.contains(access)) {
          String field = accessedField(access, fieldIds, shadowed);
          if (field != null) {
            read.add(field);
          }
        }
      }
      for (String name : read) {
        out.add(Relationship.deterministic(fromId, fieldIds.get(name), RelationType.READS));
      }
      for (String name : written) {
        out.add(Relationship.deterministic(fromId, fieldIds.get(name), RelationType.WRITES));
      }
    }
  }

  /** The type field an expression touches: a {@code this.x}, or a bare name nothing shadows. */
  private static String accessedField(
      Expression expression, Map<String, String> fieldIds, Set<String> shadowed) {
    if (expression instanceof FieldAccessExpr access && access.getScope() instanceof ThisExpr) {
      String name = access.getNameAsString();
      return fieldIds.containsKey(name) ? name : null;
    }
    if (expression instanceof NameExpr name) {
      String id = name.getNameAsString();
      return fieldIds.containsKey(id) && !shadowed.contains(id) ? id : null;
    }
    return null;
  }

  /** Names bound by any parameter or local in the callable — a bare match is not surely a field. */
  private static Set<String> shadowedNames(CallableDeclaration<?> callable) {
    Set<String> names = new HashSet<>();
    for (Parameter parameter : callable.findAll(Parameter.class)) {
      names.add(parameter.getNameAsString());
    }
    for (VariableDeclarator variable : callable.findAll(VariableDeclarator.class)) {
      names.add(variable.getNameAsString());
    }
    return names;
  }

  private static boolean incrementsOrDecrements(UnaryExpr unary) {
    return switch (unary.getOperator()) {
      case PREFIX_INCREMENT, PREFIX_DECREMENT, POSTFIX_INCREMENT, POSTFIX_DECREMENT -> true;
      default -> false;
    };
  }

  private static List<CallableDeclaration<?>> callables(TypeDeclaration<?> type) {
    List<CallableDeclaration<?>> callables = new ArrayList<>(type.getMethods());
    callables.addAll(type.getConstructors());
    return callables;
  }

  /** The fully-qualified name a type reference points at, via imports or the same package. */
  private static String fqnOf(
      ClassOrInterfaceType reference, String packageName, Map<String, String> importsByName) {
    return fqnOfName(reference.getNameWithScope(), packageName, importsByName);
  }

  /** The fully-qualified name a textual type name points at, via imports or the same package. */
  private static String fqnOfName(
      String referenced, String packageName, Map<String, String> importsByName) {
    if (referenced.contains(".")) {
      return referenced;
    }
    if (importsByName.containsKey(referenced)) {
      return importsByName.get(referenced);
    }
    // Not imported: a simple name resolves against the file's own package.
    return packageName.isEmpty() ? referenced : packageName + "." + referenced;
  }

  /**
   * Calls to a sibling method of the same type, where the callee resolves unambiguously by name.
   */
  private void emitSameTypeCalls(
      TypeDeclaration<?> type,
      String sourceId,
      String path,
      String qualifiedName,
      List<Relationship> out) {
    Map<String, List<String>> methodsByName = new HashMap<>();
    for (MethodDeclaration method : type.getMethods()) {
      String id = callableEntityId(sourceId, path, qualifiedName, method);
      methodsByName.computeIfAbsent(method.getNameAsString(), k -> new ArrayList<>()).add(id);
    }
    for (MethodDeclaration method : type.getMethods()) {
      String fromId = callableEntityId(sourceId, path, qualifiedName, method);
      for (MethodCallExpr call : method.findAll(MethodCallExpr.class)) {
        if (call.getScope().isPresent() && !(call.getScope().get() instanceof ThisExpr)) {
          continue; // qualified call on another object - needs type inference, deferred
        }
        List<String> candidates = methodsByName.get(call.getNameAsString());
        if (candidates != null && candidates.size() == 1 && !candidates.get(0).equals(fromId)) {
          out.add(Relationship.deterministic(fromId, candidates.get(0), RelationType.CALLS));
        }
      }
    }
  }

  private static String callableEntityId(
      String sourceId, String path, String qualifiedName, CallableDeclaration<?> callable) {
    return CodeEntity.deriveId(
        sourceId, path, qualifiedName + "#" + callable.getDeclarationAsString(false, false, false));
  }

  private static String fieldEntityId(
      String sourceId, String path, String qualifiedName, String fieldName) {
    return CodeEntity.deriveId(sourceId, path, qualifiedName + "#" + fieldName);
  }

  private static String qualified(String enclosingQualifier, String name) {
    return enclosingQualifier.isEmpty() ? name : enclosingQualifier + "." + name;
  }

  /**
   * A reference gathered before resolution: the source entity, the target name, and the edge type.
   */
  private record PendingRef(String fromId, String fqn, RelationType type) {}
}
