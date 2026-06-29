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
 * Structure-aware chunker for documents (Markdown, PDF/Office text, plain text). It first splits
 * the text into sections at every Markdown heading so a chunk never crosses a heading, then fills
 * each section with chunks up to {@code maxTokens} using a recursive boundary preference (paragraph
 * → line → sentence → word → character) and a configurable token-level overlap carried between
 * adjacent chunks. Splitting works on character offsets into the original text, so every chunk
 * keeps an exact 1-based source line range.
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
    List<Chunk> chunks = new ArrayList<>();
    for (Section section : sections(artifact.text())) {
      for (int[] span : split(section.text(), config.maxTokens(), config.overlap())) {
        int[] trimmed = trim(section.text(), span[0], span[1]);
        if (trimmed == null) {
          continue;
        }
        int lineStart = section.startLine() + countNewlines(section.text(), 0, trimmed[0]);
        int lineEnd = lineStart + countNewlines(section.text(), trimmed[0], trimmed[1]);
        chunks.add(
            ChunkBuilder.build(
                artifact,
                ChunkType.DOC,
                section.text().substring(trimmed[0], trimmed[1]),
                lineStart,
                lineEnd,
                null));
      }
    }
    return ChunkingResult.ofChunks(chunks);
  }

  /**
   * Splits the document into sections, each starting at a Markdown heading (or the document head).
   */
  private static List<Section> sections(String text) {
    String[] lines = text.split("\n", -1);
    List<Integer> starts = new ArrayList<>();
    starts.add(0);
    for (int i = 1; i < lines.length; i++) {
      if (HEADING.matcher(stripCarriageReturn(lines[i])).matches()) {
        starts.add(i);
      }
    }
    List<Section> sections = new ArrayList<>();
    for (int s = 0; s < starts.size(); s++) {
      int from = starts.get(s);
      int to = s + 1 < starts.size() ? starts.get(s + 1) : lines.length;
      StringBuilder sb = new StringBuilder();
      for (int i = from; i < to; i++) {
        if (i > from) {
          sb.append('\n');
        }
        sb.append(stripCarriageReturn(lines[i]));
      }
      if (!sb.toString().isBlank()) {
        sections.add(new Section(from + 1, sb.toString()));
      }
    }
    return sections;
  }

  /** Returns chunk spans {@code [start, end)} over the section text, with token-level overlap. */
  private static List<int[]> split(String text, int maxTokens, int overlap) {
    List<int[]> spans = new ArrayList<>();
    int n = text.length();
    if (TokenCounter.count(text) <= maxTokens) {
      spans.add(new int[] {0, n});
      return spans;
    }
    int[] paragraph = breaks(text, BreakType.PARAGRAPH);
    int[] line = breaks(text, BreakType.LINE);
    int[] sentence = breaks(text, BreakType.SENTENCE);
    int[] word = breaks(text, BreakType.WORD);

    int start = 0;
    while (start < n) {
      if (TokenCounter.count(text.substring(start, n)) <= maxTokens) {
        spans.add(new int[] {start, n});
        break;
      }
      int end = farthest(text, paragraph, start, n, maxTokens);
      if (end <= start) {
        end = farthest(text, line, start, n, maxTokens);
      }
      if (end <= start) {
        end = farthest(text, sentence, start, n, maxTokens);
      }
      if (end <= start) {
        end = farthest(text, word, start, n, maxTokens);
      }
      if (end <= start) {
        end = charCut(text, start, n, maxTokens);
      }
      spans.add(new int[] {start, end});
      if (end >= n) {
        break;
      }
      int next = overlapStart(text, word, start, end, overlap);
      start = next > start ? next : end;
    }
    return spans;
  }

  /** The largest break index after {@code start} whose span still fits the token budget, or -1. */
  private static int farthest(String text, int[] breaks, int start, int n, int maxTokens) {
    int best = -1;
    for (int b : breaks) {
      if (b <= start) {
        continue;
      }
      if (b > n || TokenCounter.count(text.substring(start, b)) > maxTokens) {
        break; // token count grows with b, so nothing further fits either
      }
      best = b;
    }
    return best;
  }

  /** Last resort when a single unbroken unit exceeds the budget: cut on a character boundary. */
  private static int charCut(String text, int start, int n, int maxTokens) {
    int best = start + 1;
    for (int e = start + 1; e <= n; e++) {
      if (TokenCounter.count(text.substring(start, e)) > maxTokens) {
        break;
      }
      best = e;
    }
    return best;
  }

  /** The earliest word boundary whose tail up to {@code end} fits the overlap budget. */
  private static int overlapStart(String text, int[] word, int start, int end, int overlap) {
    if (overlap <= 0) {
      return end;
    }
    for (int b : word) {
      if (b <= start) {
        continue;
      }
      if (b >= end) {
        break;
      }
      if (TokenCounter.count(text.substring(b, end)) <= overlap) {
        return b;
      }
    }
    return end;
  }

  private enum BreakType {
    PARAGRAPH,
    LINE,
    SENTENCE,
    WORD
  }

  /** Candidate cut indices of the given kind (the index just after the boundary character). */
  private static int[] breaks(String text, BreakType type) {
    List<Integer> positions = new ArrayList<>();
    for (int i = 1; i <= text.length(); i++) {
      char prev = text.charAt(i - 1);
      boolean isBreak =
          switch (type) {
            case PARAGRAPH -> prev == '\n' && i >= 2 && text.charAt(i - 2) == '\n';
            case LINE -> prev == '\n';
            case SENTENCE ->
                Character.isWhitespace(prev) && i >= 2 && isSentenceEnd(text.charAt(i - 2));
            case WORD -> Character.isWhitespace(prev);
          };
      if (isBreak) {
        positions.add(i);
      }
    }
    return positions.stream().mapToInt(Integer::intValue).toArray();
  }

  private static boolean isSentenceEnd(char c) {
    return c == '.' || c == '!' || c == '?';
  }

  /** Trims whitespace inward; returns {@code null} when the span is all whitespace. */
  private static int[] trim(String text, int start, int end) {
    int s = start;
    int e = end;
    while (s < e && Character.isWhitespace(text.charAt(s))) {
      s++;
    }
    while (e > s && Character.isWhitespace(text.charAt(e - 1))) {
      e--;
    }
    return s < e ? new int[] {s, e} : null;
  }

  private static int countNewlines(String text, int from, int to) {
    int count = 0;
    for (int i = from; i < to; i++) {
      if (text.charAt(i) == '\n') {
        count++;
      }
    }
    return count;
  }

  private static String stripCarriageReturn(String line) {
    return line.endsWith("\r") ? line.substring(0, line.length() - 1) : line;
  }

  /** A heading-delimited section with its 1-based first source line. */
  private record Section(int startLine, String text) {}
}
