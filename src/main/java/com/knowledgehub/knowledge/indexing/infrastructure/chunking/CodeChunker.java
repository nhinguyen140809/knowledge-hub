package com.knowledgehub.knowledge.indexing.infrastructure.chunking;

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
import com.knowledgehub.knowledge.indexing.domain.Chunk;
import com.knowledgehub.knowledge.indexing.domain.ChunkConfig;
import com.knowledgehub.knowledge.indexing.domain.ChunkType;
import com.knowledgehub.knowledge.indexing.domain.Chunker;
import com.knowledgehub.knowledge.indexing.domain.ChunkingResult;
import com.knowledgehub.knowledge.indexing.domain.CodeEntity;
import com.knowledgehub.knowledge.indexing.domain.CodeEntityLevel;
import com.knowledgehub.knowledge.ingestion.domain.RawArtifact;
import com.knowledgehub.shared.id.IdFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * AST-aware chunker for Java source (via JavaParser). It cuts on declaration boundaries — one chunk
 * per method/constructor (never split mid-function, even past the token budget), plus a "shell"
 * chunk per type holding the class context (signature + fields) with member bodies removed so the
 * text is not duplicated. It also extracts the {@link CodeEntity} hierarchy (type → methods/fields)
 * for graph linking. Leading Javadoc/comments are kept with each chunk to strengthen the signal.
 *
 * <p>Highest precedence so it wins over the document fallback for {@code .java} files. Other
 * languages fall through to the document chunker until their own AST chunker is added.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CodeChunker implements Chunker {

  @Override
  public boolean supports(RawArtifact artifact) {
    return artifact.text() != null && artifact.path().toLowerCase(Locale.ROOT).endsWith(".java");
  }

  @Override
  public ChunkingResult chunk(RawArtifact artifact, ChunkConfig config) {
    ParseResult<CompilationUnit> parsed = new JavaParser().parse(artifact.text());
    CompilationUnit cu =
        parsed.isSuccessful() ? parsed.getResult().orElseThrow() : fail(artifact.path(), parsed);

    String[] lines = artifact.text().split("\n", -1);
    String packageName = cu.getPackageDeclaration().map(p -> p.getNameAsString()).orElse("");

    List<Chunk> chunks = new ArrayList<>();
    List<CodeEntity> entities = new ArrayList<>();
    for (TypeDeclaration<?> type : cu.getTypes()) {
      processType(artifact, lines, packageName, null, type, chunks, entities);
    }
    return new ChunkingResult(chunks, entities);
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
      List<Chunk> chunks,
      List<CodeEntity> entities) {
    String sourceId = artifact.provenance().sourceId();
    String path = artifact.path();
    String fileId = IdFactory.fileId(sourceId, path);
    String qualifiedName =
        enclosingQualifier.isEmpty()
            ? type.getNameAsString()
            : enclosingQualifier + "." + type.getNameAsString();
    String entityId = IdFactory.entityId(sourceId, path, qualifiedName);
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
      String signature = callable.getDeclarationAsString(false, false, false);
      CodeEntityLevel memberLevel =
          callable instanceof ConstructorDeclaration
              ? CodeEntityLevel.CONSTRUCTOR
              : CodeEntityLevel.METHOD;
      String memberId = IdFactory.entityId(sourceId, path, qualifiedName + "#" + signature);
      entities.add(
          new CodeEntity(
              memberId,
              sourceId,
              fileId,
              entityId,
              memberLevel,
              memberName,
              signature,
              range.begin.line,
              range.end.line));
      chunks.add(
          ChunkBuilder.build(
              artifact,
              ChunkType.CODE,
              slice(lines, range),
              range.begin.line,
              range.end.line,
              memberId));
    }

    for (FieldDeclaration field : type.getFields()) {
      Range range = field.getRange().orElseThrow();
      String fieldName = field.getVariable(0).getNameAsString();
      entities.add(
          new CodeEntity(
              IdFactory.entityId(sourceId, path, qualifiedName + "#" + fieldName),
              sourceId,
              fileId,
              entityId,
              CodeEntityLevel.FIELD,
              fieldName,
              field.toString(),
              range.begin.line,
              range.end.line));
    }

    for (Node member : type.getMembers()) {
      if (member instanceof TypeDeclaration<?> nested) {
        Range range = nested.getRange().orElseThrow();
        mark(excluded, range);
        processType(artifact, lines, qualifiedName, entityId, nested, chunks, entities);
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
