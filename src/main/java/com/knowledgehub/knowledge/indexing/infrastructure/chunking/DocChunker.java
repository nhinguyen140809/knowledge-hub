package com.knowledgehub.knowledge.indexing.infrastructure.chunking;

import com.knowledgehub.knowledge.indexing.domain.Chunk;
import com.knowledgehub.knowledge.indexing.domain.ChunkConfig;
import com.knowledgehub.knowledge.indexing.domain.ChunkType;
import com.knowledgehub.knowledge.indexing.domain.Chunker;
import com.knowledgehub.knowledge.indexing.domain.ChunkingResult;
import com.knowledgehub.knowledge.ingestion.domain.RawArtifact;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Structure-aware chunker for documents (Markdown, PDF/Office text, plain text). It splits the text
 * into paragraph blocks, forces a chunk boundary at every Markdown heading so chunks stay aligned
 * to sections, and greedily packs blocks up to {@code maxTokens} carrying a configurable token
 * overlap between adjacent chunks. A single block larger than the budget is split by words so no
 * chunk runs far past the target.
 *
 * <p>Lowest precedence so it acts as the fallback once language-specific chunkers have had their
 * say (it accepts any artifact that carries text).
 */
@Component
@Order(Ordered.LOWEST_PRECEDENCE)
public class DocChunker implements Chunker {

  private static final Pattern HEADING = Pattern.compile("^#{1,6}\\s.*");

  @Override
  public boolean supports(RawArtifact artifact) {
    return artifact.text() != null && !artifact.text().isBlank();
  }

  @Override
  public ChunkingResult chunk(RawArtifact artifact, ChunkConfig config) {
    List<Block> blocks = toBlocks(artifact.text());
    List<Chunk> chunks = new ArrayList<>();

    List<Block> current = new ArrayList<>();
    int currentTokens = 0;
    for (Block block : blocks) {
      boolean boundary = block.heading() || currentTokens + block.tokens() > config.maxTokens();
      if (boundary && !current.isEmpty()) {
        chunks.add(emit(artifact, current));
        current = carryOverlap(current, config.overlap());
        currentTokens = sumTokens(current);
      }
      if (block.tokens() > config.maxTokens()) {
        // Indivisible-by-paragraph but too big: split this block by words.
        if (!current.isEmpty()) {
          chunks.add(emit(artifact, current));
          current = new ArrayList<>();
          currentTokens = 0;
        }
        for (Block piece : splitOversized(block, config.maxTokens())) {
          chunks.add(emit(artifact, List.of(piece)));
        }
        continue;
      }
      current.add(block);
      currentTokens += block.tokens();
    }
    if (!current.isEmpty()) {
      chunks.add(emit(artifact, current));
    }
    return ChunkingResult.ofChunks(chunks);
  }

  /** Groups lines into paragraph blocks (separated by blank lines or a heading line). */
  private static List<Block> toBlocks(String text) {
    String[] lines = text.split("\n", -1);
    List<Block> blocks = new ArrayList<>();
    List<String> buffer = new ArrayList<>();
    int blockStart = 0;
    for (int i = 0; i < lines.length; i++) {
      String line = stripCarriageReturn(lines[i]);
      boolean blank = line.isBlank();
      boolean heading = HEADING.matcher(line).matches();
      if ((blank || heading) && !buffer.isEmpty()) {
        blocks.add(block(buffer, blockStart, i));
        buffer = new ArrayList<>();
      }
      if (blank) {
        continue;
      }
      if (buffer.isEmpty()) {
        blockStart = i + 1; // 1-based first content line of this block
      }
      buffer.add(line);
    }
    if (!buffer.isEmpty()) {
      blocks.add(block(buffer, blockStart, lines.length));
    }
    return blocks;
  }

  private static Block block(List<String> lines, int startLine, int endLine) {
    String text = String.join("\n", lines);
    boolean heading = HEADING.matcher(lines.get(0)).matches();
    return new Block(text, startLine, endLine, TokenEstimator.estimate(text), heading);
  }

  /** Carries trailing blocks whose combined tokens fit the overlap budget into the next chunk. */
  private static List<Block> carryOverlap(List<Block> emitted, int overlap) {
    List<Block> carried = new ArrayList<>();
    if (overlap <= 0) {
      return carried;
    }
    int tokens = 0;
    for (int i = emitted.size() - 1; i >= 0; i--) {
      Block block = emitted.get(i);
      if (tokens + block.tokens() > overlap) {
        break;
      }
      carried.add(0, block);
      tokens += block.tokens();
    }
    return carried;
  }

  private static List<Block> splitOversized(Block block, int maxTokens) {
    List<Block> pieces = new ArrayList<>();
    String[] words = block.text().split("\\s+");
    StringBuilder buffer = new StringBuilder();
    for (String word : words) {
      if (buffer.length() > 0 && TokenEstimator.estimate(buffer + " " + word) > maxTokens) {
        pieces.add(block.withText(buffer.toString()));
        buffer.setLength(0);
      }
      if (buffer.length() > 0) {
        buffer.append(' ');
      }
      buffer.append(word);
    }
    if (buffer.length() > 0) {
      pieces.add(block.withText(buffer.toString()));
    }
    return pieces;
  }

  private static Chunk emit(RawArtifact artifact, List<Block> blocks) {
    String text = String.join("\n\n", blocks.stream().map(Block::text).toList());
    int lineStart = blocks.get(0).startLine();
    int lineEnd = blocks.get(blocks.size() - 1).endLine();
    return ChunkBuilder.build(artifact, ChunkType.DOC, text, lineStart, lineEnd, null);
  }

  private static int sumTokens(List<Block> blocks) {
    return blocks.stream().mapToInt(Block::tokens).sum();
  }

  private static String stripCarriageReturn(String line) {
    return line.endsWith("\r") ? line.substring(0, line.length() - 1) : line;
  }

  /** A paragraph (or heading) with its 1-based, inclusive source line range. */
  private record Block(String text, int startLine, int endLine, int tokens, boolean heading) {
    Block withText(String newText) {
      return new Block(newText, startLine, endLine, TokenEstimator.estimate(newText), heading);
    }
  }
}
