package com.knowledgehub.knowledge.graph.infrastructure.extract;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.EnumDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.ThisExpr;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.knowledgehub.knowledge.graph.domain.EntityResolver;
import com.knowledgehub.knowledge.graph.domain.RelationType;
import com.knowledgehub.knowledge.graph.domain.Relationship;
import com.knowledgehub.knowledge.graph.domain.ResolutionScope;
import com.knowledgehub.knowledge.graph.domain.StructuralExtractor;
import com.knowledgehub.knowledge.ingestion.domain.RawArtifact;
import com.knowledgehub.shared.id.IdFactory;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Reads structural relationships out of Java source via JavaParser. It emits {@code IMPORTS} (file
 * imports resolved to entities), {@code EXTENDS}/{@code IMPLEMENTS} (inheritance), and {@code
 * CALLS} for calls to a sibling method of the same type. All edges are deterministic (confidence
 * 1); a reference that does not resolve to an indexed entity is dropped rather than guessed.
 *
 * <p>Targets are resolved through the {@link EntityResolver}, so an import or supertype that lives
 * in another source links across source boundaries. Calls across types (which need full type
 * inference) and {@code OVERRIDES} are intentionally left to a later, symbol-solver-backed pass.
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

    // Same-type calls resolve locally; imports and inheritance are gathered then resolved in one
    // batch, so a file is at most a single resolver round-trip rather than one per reference.
    List<Relationship> out = new ArrayList<>();
    List<PendingRef> refs = new ArrayList<>();
    for (TypeDeclaration<?> type : cu.getTypes()) {
      String topId =
          IdFactory.entityId(sourceId, path, qualified(packageName, type.getNameAsString()));
      collectImports(cu, topId, refs);
      collectType(type, packageName, sourceId, path, importsByName, refs, out);
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
        out.add(Relationship.structural(ref.fromId(), toId, ref.type()));
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

  private void collectType(
      TypeDeclaration<?> type,
      String enclosingQualifier,
      String sourceId,
      String path,
      Map<String, String> importsByName,
      List<PendingRef> refs,
      List<Relationship> out) {
    String qualifiedName = qualified(enclosingQualifier, type.getNameAsString());
    String typeId = IdFactory.entityId(sourceId, path, qualifiedName);

    collectInheritance(type, typeId, enclosingQualifier, importsByName, refs);
    emitSameTypeCalls(type, sourceId, path, qualifiedName, out);

    for (Node member : type.getMembers()) {
      if (member instanceof TypeDeclaration<?> nested) {
        collectType(nested, qualifiedName, sourceId, path, importsByName, refs, out);
      }
    }
  }

  private static void collectInheritance(
      TypeDeclaration<?> type,
      String typeId,
      String packageScope,
      Map<String, String> importsByName,
      List<PendingRef> refs) {
    if (type instanceof ClassOrInterfaceDeclaration cls) {
      for (ClassOrInterfaceType ext : cls.getExtendedTypes()) {
        refs.add(
            new PendingRef(typeId, fqnOf(ext, packageScope, importsByName), RelationType.EXTENDS));
      }
      for (ClassOrInterfaceType impl : cls.getImplementedTypes()) {
        refs.add(
            new PendingRef(
                typeId, fqnOf(impl, packageScope, importsByName), RelationType.IMPLEMENTS));
      }
    } else if (type instanceof EnumDeclaration en) {
      for (ClassOrInterfaceType impl : en.getImplementedTypes()) {
        refs.add(
            new PendingRef(
                typeId, fqnOf(impl, packageScope, importsByName), RelationType.IMPLEMENTS));
      }
    }
  }

  /** The fully-qualified name a type reference points at, via imports or the same package. */
  private static String fqnOf(
      ClassOrInterfaceType reference, String packageScope, Map<String, String> importsByName) {
    String referenced = reference.getNameWithScope();
    if (referenced.contains(".")) {
      return referenced;
    }
    if (importsByName.containsKey(referenced)) {
      return importsByName.get(referenced);
    }
    // Same package when not imported (the package scope is the enclosing qualifier of the type).
    return packageScope.isEmpty() ? referenced : packageScope + "." + referenced;
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
      String id = methodEntityId(sourceId, path, qualifiedName, method);
      methodsByName.computeIfAbsent(method.getNameAsString(), k -> new ArrayList<>()).add(id);
    }
    for (MethodDeclaration method : type.getMethods()) {
      String fromId = methodEntityId(sourceId, path, qualifiedName, method);
      for (MethodCallExpr call : method.findAll(MethodCallExpr.class)) {
        if (call.getScope().isPresent() && !(call.getScope().get() instanceof ThisExpr)) {
          continue; // qualified call on another object - needs type inference, deferred
        }
        List<String> candidates = methodsByName.get(call.getNameAsString());
        if (candidates != null && candidates.size() == 1 && !candidates.get(0).equals(fromId)) {
          out.add(Relationship.structural(fromId, candidates.get(0), RelationType.CALLS));
        }
      }
    }
  }

  private static String methodEntityId(
      String sourceId, String path, String qualifiedName, MethodDeclaration method) {
    return IdFactory.entityId(
        sourceId, path, qualifiedName + "#" + method.getDeclarationAsString(false, false, false));
  }

  private static String qualified(String enclosingQualifier, String name) {
    return enclosingQualifier.isEmpty() ? name : enclosingQualifier + "." + name;
  }

  /**
   * A reference gathered before resolution: the source entity, the target name, and the edge type.
   */
  private record PendingRef(String fromId, String fqn, RelationType type) {}
}
