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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Reads structural relationships out of Java source via JavaParser. It emits {@code IMPORTS} (file
 * imports resolved to entities), {@code EXTENDS}/{@code IMPLEMENTS} (inheritance), and {@code CALLS}
 * for calls to a sibling method of the same type. All edges are deterministic (confidence 1); a
 * reference that does not resolve to an indexed entity is dropped rather than guessed.
 *
 * <p>Targets are resolved through the {@link EntityResolver}, so an import or supertype that lives in
 * another source links across source boundaries. Calls across types (which need full type inference)
 * and {@code OVERRIDES} are intentionally left to a later, symbol-solver-backed pass.
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
    ResolutionScope scope = new ResolutionScope(sourceId);
    Map<String, String> importsByName = importsByName(cu);

    List<Relationship> out = new ArrayList<>();
    emitImports(cu, sourceId, path, packageName, scope, out);
    for (TypeDeclaration<?> type : cu.getTypes()) {
      processType(type, packageName, sourceId, path, importsByName, scope, out);
    }
    return out;
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

  /** A file's imports become IMPORTS edges from its first top-level type to the imported entities. */
  private void emitImports(
      CompilationUnit cu,
      String sourceId,
      String path,
      String packageName,
      ResolutionScope scope,
      List<Relationship> out) {
    if (cu.getTypes().isEmpty()) {
      return;
    }
    String fromId =
        IdFactory.entityId(sourceId, path, qualified(packageName, cu.getType(0).getNameAsString()));
    for (ImportDeclaration imp : cu.getImports()) {
      if (imp.isAsterisk() || imp.isStatic()) {
        continue;
      }
      resolver
          .resolve(imp.getNameAsString(), scope)
          .ifPresent(toId -> out.add(Relationship.structural(fromId, toId, RelationType.IMPORTS)));
    }
  }

  private void processType(
      TypeDeclaration<?> type,
      String enclosingQualifier,
      String sourceId,
      String path,
      Map<String, String> importsByName,
      ResolutionScope scope,
      List<Relationship> out) {
    String qualifiedName = qualified(enclosingQualifier, type.getNameAsString());
    String typeId = IdFactory.entityId(sourceId, path, qualifiedName);

    emitInheritance(type, typeId, enclosingQualifier, importsByName, scope, out);
    emitSameTypeCalls(type, sourceId, path, qualifiedName, out);

    for (Node member : type.getMembers()) {
      if (member instanceof TypeDeclaration<?> nested) {
        processType(nested, qualifiedName, sourceId, path, importsByName, scope, out);
      }
    }
  }

  private void emitInheritance(
      TypeDeclaration<?> type,
      String typeId,
      String packageScope,
      Map<String, String> importsByName,
      ResolutionScope scope,
      List<Relationship> out) {
    if (type instanceof ClassOrInterfaceDeclaration cls) {
      for (ClassOrInterfaceType ext : cls.getExtendedTypes()) {
        link(typeId, ext, RelationType.EXTENDS, packageScope, importsByName, scope, out);
      }
      for (ClassOrInterfaceType impl : cls.getImplementedTypes()) {
        link(typeId, impl, RelationType.IMPLEMENTS, packageScope, importsByName, scope, out);
      }
    } else if (type instanceof EnumDeclaration en) {
      for (ClassOrInterfaceType impl : en.getImplementedTypes()) {
        link(typeId, impl, RelationType.IMPLEMENTS, packageScope, importsByName, scope, out);
      }
    }
  }

  private void link(
      String fromId,
      ClassOrInterfaceType reference,
      RelationType type,
      String packageScope,
      Map<String, String> importsByName,
      ResolutionScope scope,
      List<Relationship> out) {
    String referenced = reference.getNameWithScope();
    String fqn;
    if (referenced.contains(".")) {
      fqn = referenced;
    } else if (importsByName.containsKey(referenced)) {
      fqn = importsByName.get(referenced);
    } else {
      // Same package when not imported (the package scope is the enclosing qualifier of the type).
      fqn = packageScope.isEmpty() ? referenced : packageScope + "." + referenced;
    }
    resolver
        .resolve(fqn, scope)
        .ifPresent(toId -> out.add(Relationship.structural(fromId, toId, type)));
  }

  /** Calls to a sibling method of the same type, where the callee resolves unambiguously by name. */
  private void emitSameTypeCalls(
      TypeDeclaration<?> type, String sourceId, String path, String qualifiedName, List<Relationship> out) {
    Map<String, List<String>> methodsByName = new HashMap<>();
    for (MethodDeclaration method : type.getMethods()) {
      String id =
          IdFactory.entityId(
              sourceId, path, qualifiedName + "#" + method.getDeclarationAsString(false, false, false));
      methodsByName.computeIfAbsent(method.getNameAsString(), k -> new ArrayList<>()).add(id);
    }
    for (MethodDeclaration method : type.getMethods()) {
      String fromId =
          IdFactory.entityId(
              sourceId, path, qualifiedName + "#" + method.getDeclarationAsString(false, false, false));
      for (MethodCallExpr call : method.findAll(MethodCallExpr.class)) {
        if (call.getScope().isPresent() && !(call.getScope().get() instanceof ThisExpr)) {
          continue; // qualified call on another object — needs type inference, deferred
        }
        List<String> candidates = methodsByName.get(call.getNameAsString());
        if (candidates != null && candidates.size() == 1) {
          out.add(Relationship.structural(fromId, candidates.get(0), RelationType.CALLS));
        }
      }
    }
  }

  private static String qualified(String enclosingQualifier, String name) {
    return enclosingQualifier.isEmpty() ? name : enclosingQualifier + "." + name;
  }
}
