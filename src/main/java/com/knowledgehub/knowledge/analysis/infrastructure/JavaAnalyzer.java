package com.knowledgehub.knowledge.analysis.infrastructure;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.Range;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.CallableDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.EnumDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.knowledgehub.knowledge.analysis.domain.AnalysisResult;
import com.knowledgehub.knowledge.analysis.domain.Chunk;
import com.knowledgehub.knowledge.analysis.domain.ChunkConfig;
import com.knowledgehub.knowledge.analysis.domain.ChunkType;
import com.knowledgehub.knowledge.analysis.domain.CodeEntity;
import com.knowledgehub.knowledge.analysis.domain.CodeEntityLevel;
import com.knowledgehub.knowledge.infrastructure.lang.JavaLanguage;
import com.knowledgehub.knowledge.ingestion.domain.RawArtifact;
import com.knowledgehub.shared.id.IdFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * AST-aware analyzer for Java source (via JavaParser). It cuts on declaration boundaries — one
 * chunk per method/constructor (never split mid-function, even past the token budget), plus a
 * "shell" chunk per type holding the class context (signature + fields) with member bodies removed
 * so the text is not duplicated. The same single parse also yields the {@link CodeEntity} hierarchy
 * (type → methods/fields) and, via {@link JavaRelationCollector}, the relationships the syntax
 * decides — resolved same-file edges plus pending references for the linking step. Leading
 * Javadoc/comments are kept with each chunk to strengthen the signal.
 *
 * <p>The Java implementation of the code-analyzer strategy: it binds to {@link JavaLanguage} (so
 * the {@code .java} extension it claims comes from that one registration) and inherits the
 * extension test from {@link AbstractCodeAnalyzer}. Another language is one more subclass with its
 * own parser, never a change here. Highest precedence so it wins over the document fallback for
 * {@code .java} files.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class JavaAnalyzer extends AbstractCodeAnalyzer {

  private static final Logger log = LoggerFactory.getLogger(JavaAnalyzer.class);

  public JavaAnalyzer(JavaLanguage language) {
    super(language);
  }

  @Override
  public AnalysisResult analyze(RawArtifact artifact, ChunkConfig config) {
    ParseResult<CompilationUnit> parsed = new JavaParser().parse(artifact.text());
    CompilationUnit cu =
        parsed.isSuccessful() ? parsed.getResult().orElseThrow() : fail(artifact.path(), parsed);

    String[] lines = artifact.text().split("\n", -1);
    String packageName = cu.getPackageDeclaration().map(p -> p.getNameAsString()).orElse("");

    List<Chunk> chunks = new ArrayList<>();
    List<CodeEntity> entities = new ArrayList<>();
    for (TypeDeclaration<?> type : cu.getTypes()) {
      processType(artifact, lines, packageName, null, type, config.maxTokens(), chunks, entities);
    }
    // The relation walk shares this parse: same CompilationUnit, same id derivation.
    JavaRelationCollector.Collected collected =
        JavaRelationCollector.collect(cu, artifact.provenance().sourceId(), artifact.path());
    return new AnalysisResult(
        chunks, entities, collected.relations(), collected.pendingReferences());
  }

  private static CompilationUnit fail(String path, ParseResult<CompilationUnit> parsed) {
    throw new IllegalArgumentException("unparseable Java " + path + ": " + parsed.getProblems());
  }

  private void processType(
      RawArtifact artifact,
      String[] lines,
      String enclosingQualifier,
      String parentEntityId,
      TypeDeclaration<?> type,
      int maxTokens,
      List<Chunk> chunks,
      List<CodeEntity> entities) {
    String sourceId = artifact.provenance().sourceId();
    String path = artifact.path();
    String fileId = IdFactory.fileId(sourceId, path);
    String qualifiedName = JavaEntityIds.qualified(enclosingQualifier, type.getNameAsString());
    String entityId = JavaEntityIds.typeId(sourceId, path, qualifiedName);
    Range typeRange = type.getRange().orElseThrow();
    CodeEntityLevel level = levelOf(type);

    entities.add(
        new CodeEntity(
            entityId,
            sourceId,
            fileId,
            parentEntityId,
            level,
            type.getNameAsString(),
            qualifiedName,
            keyword(level) + " " + type.getNameAsString(),
            typeRange.begin.line,
            typeRange.end.line));

    // Lines covered by members that get their own chunk / are recursed into, so the shell excludes
    // them.
    boolean[] excluded = new boolean[lines.length + 2];

    for (CallableDeclaration<?> callable : callables(type)) {
      Range range = withLeadingComment(callable);
      mark(excluded, range);
      String memberName = callable.getNameAsString();
      String signature = JavaEntityIds.signature(callable);
      CodeEntityLevel memberLevel =
          callable instanceof ConstructorDeclaration
              ? CodeEntityLevel.CONSTRUCTOR
              : CodeEntityLevel.METHOD;
      String memberQualifiedName = qualifiedName + "#" + signature;
      String memberId = JavaEntityIds.callableId(sourceId, path, qualifiedName, callable);
      entities.add(
          new CodeEntity(
              memberId,
              sourceId,
              fileId,
              entityId,
              memberLevel,
              memberName,
              memberQualifiedName,
              signature,
              range.begin.line,
              range.end.line));
      Chunk chunk =
          ChunkBuilder.build(
              artifact,
              ChunkType.CODE,
              slice(lines, range),
              range.begin.line,
              range.end.line,
              memberId);
      if (chunk.tokenCount() > maxTokens) {
        log.warn(
            "{} {} in {} is {} tokens, over the {}-token budget — kept as one chunk to avoid"
                + " splitting a function; it may exceed the embedding model's input limit",
            memberLevel,
            memberName,
            path,
            chunk.tokenCount(),
            maxTokens);
      }
      chunks.add(chunk);
    }

    for (FieldDeclaration field : type.getFields()) {
      Range range = field.getRange().orElseThrow();
      String modifiers =
          field.getModifiers().stream()
              .map(modifier -> modifier.getKeyword().asString())
              .collect(Collectors.joining(" "));
      for (VariableDeclarator variable : field.getVariables()) {
        String fieldName = variable.getNameAsString();
        String signature =
            (modifiers.isEmpty() ? "" : modifiers + " ")
                + variable.getTypeAsString()
                + " "
                + fieldName;
        String fieldQualifiedName = qualifiedName + "#" + fieldName;
        entities.add(
            new CodeEntity(
                JavaEntityIds.fieldId(sourceId, path, qualifiedName, fieldName),
                sourceId,
                fileId,
                entityId,
                CodeEntityLevel.FIELD,
                fieldName,
                fieldQualifiedName,
                signature,
                range.begin.line,
                range.end.line));
      }
    }

    for (Node member : type.getMembers()) {
      if (member instanceof TypeDeclaration<?> nested) {
        Range range = nested.getRange().orElseThrow();
        mark(excluded, range);
        processType(artifact, lines, qualifiedName, entityId, nested, maxTokens, chunks, entities);
      }
    }

    String shell = shell(lines, typeRange, excluded);
    if (!shell.isBlank()) {
      chunks.add(
          ChunkBuilder.build(
              artifact, ChunkType.CODE, shell, typeRange.begin.line, typeRange.end.line, entityId));
    }
  }

  private static List<CallableDeclaration<?>> callables(TypeDeclaration<?> type) {
    List<CallableDeclaration<?>> callables = new ArrayList<>();
    callables.addAll(type.getMethods());
    callables.addAll(type.getConstructors());
    return callables;
  }

  private static CodeEntityLevel levelOf(TypeDeclaration<?> type) {
    if (type instanceof EnumDeclaration) {
      return CodeEntityLevel.ENUM;
    }
    if (type instanceof ClassOrInterfaceDeclaration cls && cls.isInterface()) {
      return CodeEntityLevel.INTERFACE;
    }
    return CodeEntityLevel.CLASS;
  }

  private static String keyword(CodeEntityLevel level) {
    return switch (level) {
      case INTERFACE -> "interface";
      case ENUM -> "enum";
      default -> "class";
    };
  }

  /** A node's range extended to include its leading Javadoc/comment, when present. */
  private static Range withLeadingComment(Node node) {
    Range range = node.getRange().orElseThrow();
    return node.getComment()
        .flatMap(Node::getRange)
        .map(comment -> Range.range(comment.begin.line, 1, range.end.line, range.end.column))
        .orElse(range);
  }

  private static void mark(boolean[] excluded, Range range) {
    for (int line = range.begin.line; line <= range.end.line && line < excluded.length; line++) {
      excluded[line] = true;
    }
  }

  /** Joins the file lines within the 1-based inclusive range. */
  private static String slice(String[] lines, Range range) {
    StringBuilder sb = new StringBuilder();
    for (int line = range.begin.line; line <= range.end.line && line <= lines.length; line++) {
      if (sb.length() > 0) {
        sb.append('\n');
      }
      sb.append(stripCarriageReturn(lines[line - 1]));
    }
    return sb.toString();
  }

  /** The type's lines minus the excluded member lines — the class signature and fields. */
  private static String shell(String[] lines, Range typeRange, boolean[] excluded) {
    StringBuilder sb = new StringBuilder();
    for (int line = typeRange.begin.line;
        line <= typeRange.end.line && line <= lines.length;
        line++) {
      if (excluded[line]) {
        continue;
      }
      if (sb.length() > 0) {
        sb.append('\n');
      }
      sb.append(stripCarriageReturn(lines[line - 1]));
    }
    return sb.toString();
  }

  private static String stripCarriageReturn(String line) {
    return line.endsWith("\r") ? line.substring(0, line.length() - 1) : line;
  }
}
