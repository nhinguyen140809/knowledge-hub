package com.knowledgehub.knowledge.analysis.infrastructure;

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
import com.knowledgehub.knowledge.analysis.domain.PendingReference;
import com.knowledgehub.knowledge.domain.RelationType;
import com.knowledgehub.knowledge.domain.Relationship;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Reads relationships out of an already-parsed Java compilation unit — the relation half of {@link
 * JavaAnalyzer}, sharing its single parse. Edges whose both ends live in the file (same-type calls,
 * field reads/writes) are emitted as resolved {@link Relationship}s; every other reference —
 * imports, inheritance, overrides, annotations, throws, instantiations, field types, static-scope
 * uses — becomes a {@link PendingReference} carrying the target's fully-qualified name, to be
 * resolved by the linking step once the artifact's nodes are stored.
 *
 * <p>All emitted edges are deterministic (confidence 1): a bare name is package-qualified through
 * the file's imports or its own package, and anything that would need full type inference (calls or
 * accesses on other objects) is skipped rather than guessed. Entity ids for the from-ends come from
 * {@link JavaEntityIds}, the same derivation the chunk walk stores entities under.
 */
final class JavaRelationCollector {

  private JavaRelationCollector() {}

  /** Local relations plus pending references collected from one compilation unit. */
  record Collected(List<Relationship> relations, List<PendingReference> pendingReferences) {}

  /** Walks every top-level type and harvests its relations. */
  static Collected collect(CompilationUnit cu, String sourceId, String path) {
    String packageName = cu.getPackageDeclaration().map(p -> p.getNameAsString()).orElse("");
    Map<String, String> importsByName = importsByName(cu);

    List<Relationship> relations = new ArrayList<>();
    List<PendingReference> refs = new ArrayList<>();
    for (TypeDeclaration<?> type : cu.getTypes()) {
      String topId =
          JavaEntityIds.typeId(
              sourceId, path, JavaEntityIds.qualified(packageName, type.getNameAsString()));
      collectImports(cu, topId, refs);
      collectType(type, packageName, packageName, sourceId, path, importsByName, refs, relations);
    }
    return new Collected(relations, refs);
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
  private static void collectImports(
      CompilationUnit cu, String fromId, List<PendingReference> refs) {
    for (ImportDeclaration imp : cu.getImports()) {
      if (!imp.isAsterisk() && !imp.isStatic()) {
        refs.add(new PendingReference(fromId, imp.getNameAsString(), RelationType.IMPORTS));
      }
    }
  }

  /**
   * One type's full harvest — inheritance, overrides, annotations, per-callable deep references,
   * field types, same-type calls and field accesses — then recursion into its nested types. Local
   * edges go straight to {@code relations}; cross-file references accumulate in {@code refs}.
   */
  private static void collectType(
      TypeDeclaration<?> type,
      String enclosingQualifier,
      String packageName,
      String sourceId,
      String path,
      Map<String, String> importsByName,
      List<PendingReference> refs,
      List<Relationship> relations) {
    String qualifiedName = JavaEntityIds.qualified(enclosingQualifier, type.getNameAsString());
    String typeId = JavaEntityIds.typeId(sourceId, path, qualifiedName);

    List<String> supertypes = collectInheritance(type, typeId, packageName, importsByName, refs);
    collectOverrides(type, sourceId, path, qualifiedName, supertypes, refs);
    collectAnnotations(type.getAnnotations(), typeId, packageName, importsByName, refs);
    collectCallableRefs(type, sourceId, path, qualifiedName, packageName, importsByName, refs);
    collectFieldRefs(type, sourceId, path, qualifiedName, packageName, importsByName, refs);
    emitSameTypeCalls(type, sourceId, path, qualifiedName, relations);
    emitFieldAccess(type, sourceId, path, qualifiedName, relations);

    for (Node member : type.getMembers()) {
      if (member instanceof TypeDeclaration<?> nested) {
        collectType(
            nested, qualifiedName, packageName, sourceId, path, importsByName, refs, relations);
      }
    }
  }

  /**
   * Emits the type's EXTENDS/IMPLEMENTS references and returns every supertype's qualified name.
   */
  private static List<String> collectInheritance(
      TypeDeclaration<?> type,
      String typeId,
      String packageName,
      Map<String, String> importsByName,
      List<PendingReference> refs) {
    List<String> supertypes = new ArrayList<>();
    if (type instanceof ClassOrInterfaceDeclaration cls) {
      for (ClassOrInterfaceType ext : cls.getExtendedTypes()) {
        String fqn = fqnOf(ext, packageName, importsByName);
        supertypes.add(fqn);
        refs.add(new PendingReference(typeId, fqn, RelationType.EXTENDS));
      }
      for (ClassOrInterfaceType impl : cls.getImplementedTypes()) {
        String fqn = fqnOf(impl, packageName, importsByName);
        supertypes.add(fqn);
        refs.add(new PendingReference(typeId, fqn, RelationType.IMPLEMENTS));
      }
    } else if (type instanceof EnumDeclaration en) {
      for (ClassOrInterfaceType impl : en.getImplementedTypes()) {
        String fqn = fqnOf(impl, packageName, importsByName);
        supertypes.add(fqn);
        refs.add(new PendingReference(typeId, fqn, RelationType.IMPLEMENTS));
      }
    }
    return supertypes;
  }

  /**
   * A method carrying {@code @Override} overrides the same-signature method in a supertype; emit an
   * OVERRIDES reference to whichever supertype actually declares it (others simply do not resolve).
   */
  private static void collectOverrides(
      TypeDeclaration<?> type,
      String sourceId,
      String path,
      String qualifiedName,
      List<String> supertypes,
      List<PendingReference> refs) {
    if (supertypes.isEmpty()) {
      return;
    }
    for (MethodDeclaration method : type.getMethods()) {
      if (method.getAnnotationByName("Override").isEmpty()) {
        continue;
      }
      String fromId = JavaEntityIds.callableId(sourceId, path, qualifiedName, method);
      String signature = "#" + JavaEntityIds.signature(method);
      for (String supertype : supertypes) {
        refs.add(new PendingReference(fromId, supertype + signature, RelationType.OVERRIDES));
      }
    }
  }

  /** Annotations on a declaration become ANNOTATED_WITH references from its entity. */
  private static void collectAnnotations(
      List<AnnotationExpr> annotations,
      String fromId,
      String packageName,
      Map<String, String> importsByName,
      List<PendingReference> refs) {
    for (AnnotationExpr annotation : annotations) {
      String fqn = fqnOfName(annotation.getNameAsString(), packageName, importsByName);
      refs.add(new PendingReference(fromId, fqn, RelationType.ANNOTATED_WITH));
    }
  }

  /**
   * Per-callable deep references: annotations, declared throws, instantiations, and static-scope
   * uses of an imported type.
   */
  private static void collectCallableRefs(
      TypeDeclaration<?> type,
      String sourceId,
      String path,
      String qualifiedName,
      String packageName,
      Map<String, String> importsByName,
      List<PendingReference> refs) {
    for (CallableDeclaration<?> callable : callables(type)) {
      String fromId = JavaEntityIds.callableId(sourceId, path, qualifiedName, callable);
      collectAnnotations(callable.getAnnotations(), fromId, packageName, importsByName, refs);
      for (ReferenceType thrown : callable.getThrownExceptions()) {
        if (thrown.isClassOrInterfaceType()) {
          String fqn = fqnOf(thrown.asClassOrInterfaceType(), packageName, importsByName);
          refs.add(new PendingReference(fromId, fqn, RelationType.THROWS));
        }
      }
      Set<String> seen = new LinkedHashSet<>();
      for (ObjectCreationExpr creation : callable.findAll(ObjectCreationExpr.class)) {
        String fqn = fqnOf(creation.getType(), packageName, importsByName);
        if (seen.add(fqn)) {
          refs.add(new PendingReference(fromId, fqn, RelationType.INSTANTIATES));
        }
      }
      for (String fqn : staticScopeReferences(callable, importsByName)) {
        if (seen.add(fqn)) {
          refs.add(new PendingReference(fromId, fqn, RelationType.REFERENCES));
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
      List<PendingReference> refs) {
    for (FieldDeclaration field : type.getFields()) {
      for (VariableDeclarator variable : field.getVariables()) {
        String fieldId =
            JavaEntityIds.fieldId(sourceId, path, qualifiedName, variable.getNameAsString());
        if (variable.getType().isClassOrInterfaceType()) {
          String fqn =
              fqnOf(variable.getType().asClassOrInterfaceType(), packageName, importsByName);
          refs.add(new PendingReference(fieldId, fqn, RelationType.HAS_TYPE));
        }
        for (ObjectCreationExpr creation :
            variable
                .getInitializer()
                .map(i -> i.findAll(ObjectCreationExpr.class))
                .orElse(List.of())) {
          String fqn = fqnOf(creation.getType(), packageName, importsByName);
          refs.add(new PendingReference(fieldId, fqn, RelationType.INSTANTIATES));
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
      List<Relationship> relations) {
    Map<String, String> fieldIds = new HashMap<>();
    for (FieldDeclaration field : type.getFields()) {
      for (VariableDeclarator variable : field.getVariables()) {
        fieldIds.put(
            variable.getNameAsString(),
            JavaEntityIds.fieldId(sourceId, path, qualifiedName, variable.getNameAsString()));
      }
    }
    if (fieldIds.isEmpty()) {
      return;
    }
    for (CallableDeclaration<?> callable : callables(type)) {
      String fromId = JavaEntityIds.callableId(sourceId, path, qualifiedName, callable);
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
        relations.add(Relationship.deterministic(fromId, fieldIds.get(name), RelationType.READS));
      }
      for (String name : written) {
        relations.add(Relationship.deterministic(fromId, fieldIds.get(name), RelationType.WRITES));
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
  private static void emitSameTypeCalls(
      TypeDeclaration<?> type,
      String sourceId,
      String path,
      String qualifiedName,
      List<Relationship> relations) {
    Map<String, List<String>> methodsByName = new HashMap<>();
    for (MethodDeclaration method : type.getMethods()) {
      String id = JavaEntityIds.callableId(sourceId, path, qualifiedName, method);
      methodsByName.computeIfAbsent(method.getNameAsString(), k -> new ArrayList<>()).add(id);
    }
    for (MethodDeclaration method : type.getMethods()) {
      String fromId = JavaEntityIds.callableId(sourceId, path, qualifiedName, method);
      for (MethodCallExpr call : method.findAll(MethodCallExpr.class)) {
        if (call.getScope().isPresent() && !(call.getScope().get() instanceof ThisExpr)) {
          continue; // qualified call on another object - needs type inference, deferred
        }
        List<String> candidates = methodsByName.get(call.getNameAsString());
        if (candidates != null && candidates.size() == 1 && !candidates.get(0).equals(fromId)) {
          relations.add(Relationship.deterministic(fromId, candidates.get(0), RelationType.CALLS));
        }
      }
    }
  }
}
