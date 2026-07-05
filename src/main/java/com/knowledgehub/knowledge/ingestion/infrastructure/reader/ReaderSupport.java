package com.knowledgehub.knowledge.ingestion.infrastructure.reader;

import com.knowledgehub.knowledge.ingestion.domain.RawArtifact;
import com.vladsch.flexmark.html2md.converter.FlexmarkHtmlConverter;
import com.vladsch.flexmark.util.data.MutableDataSet;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.metadata.TikaCoreProperties;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.Parser;
import org.apache.tika.sax.ToXMLContentHandler;
import org.xml.sax.SAXException;

/** Shared helper for the document readers that produce Markdown from a rich binary format. */
final class ReaderSupport {

  // AutoDetectParser and the converter are thread-safe and stateless, so a single shared instance
  // serves every reader; only the per-parse handler and metadata are created fresh each call.
  private static final Parser PARSER = new AutoDetectParser();
  // Force ATX headings ('# Title'), not setext underlines ('Title\n====='): the document chunker
  // sections on the '#' marker, which only ATX produces.
  private static final FlexmarkHtmlConverter HTML_TO_MARKDOWN =
      FlexmarkHtmlConverter.builder(
              new MutableDataSet().set(FlexmarkHtmlConverter.SETEXT_HEADINGS, false))
          .build();

  private ReaderSupport() {}

  /**
   * Extracts a rich document (PDF, Office, HTML, …) as Markdown: Apache Tika parses the bytes to
   * structured XHTML (headings, lists, tables become elements), then Flexmark converts that HTML to
   * Markdown, so a heading survives as {@code #} for the structure-aware document chunker.
   *
   * @throws IllegalStateException if the content cannot be parsed
   */
  static String toMarkdown(RawArtifact artifact) {
    ToXMLContentHandler handler = new ToXMLContentHandler();
    Metadata metadata = new Metadata();
    // The file name lets Tika pick the right parser (.pdf, .docx, .html …) when the raw bytes alone
    // are ambiguous.
    metadata.set(TikaCoreProperties.RESOURCE_NAME_KEY, artifact.path());
    try (InputStream in = new ByteArrayInputStream(artifact.content())) {
      PARSER.parse(in, handler, metadata, new ParseContext());
    } catch (IOException | SAXException | TikaException e) {
      throw new IllegalStateException("failed to extract " + artifact.path(), e);
    }
    return HTML_TO_MARKDOWN.convert(handler.toString()).strip();
  }
}
