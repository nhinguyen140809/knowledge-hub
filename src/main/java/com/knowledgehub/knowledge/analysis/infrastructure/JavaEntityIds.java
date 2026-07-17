package com.knowledgehub.knowledge.analysis.infrastructure;

import com.github.javaparser.ast.body.CallableDeclaration;
import com.knowledgehub.knowledge.analysis.domain.CodeEntity;

/**
 * Single source of truth for how a Java declaration maps to its entity id. Both the chunk walk and
 * the relation walk in {@link JavaAnalyzer} derive ids through these helpers, so an edge's from-end
 * always lands on the id the entity was stored under — the two derivations can never drift apart.
 */
final class JavaEntityIds {

  private JavaEntityIds() {}

  /** The entity id of a type, by its package-qualified name. */
  static String typeId(String sourceId, String path, String qualifiedName) {
    return CodeEntity.deriveId(sourceId, path, qualifiedName);
  }

  /** A callable's identity signature: the declaration without modifiers or thrown exceptions. */
  static String signature(CallableDeclaration<?> callable) {
    return callable.getDeclarationAsString(false, false, false);
  }

  /** The entity id of a method or constructor within its type. */
  static String callableId(
      String sourceId, String path, String typeQualifiedName, CallableDeclaration<?> callable) {
    return CodeEntity.deriveId(sourceId, path, typeQualifiedName + "#" + signature(callable));
  }

  /** The entity id of a field within its type. */
  static String fieldId(String sourceId, String path, String typeQualifiedName, String fieldName) {
    return CodeEntity.deriveId(sourceId, path, typeQualifiedName + "#" + fieldName);
  }

  /** Joins an enclosing qualifier (package or outer type) with a simple name. */
  static String qualified(String enclosingQualifier, String name) {
    return enclosingQualifier.isEmpty() ? name : enclosingQualifier + "." + name;
  }
}
