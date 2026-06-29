package com.knowledgehub.knowledge.graph.application;

/**
 * Outcome of linking one artifact.
 *
 * @param relationshipsWritten relationships upserted into the graph (structural + accepted
 *     cross-artifact)
 * @param candidatesDropped cross-artifact candidates discarded for scoring below the confidence
 *     threshold
 */
public record LinkSummary(int relationshipsWritten, int candidatesDropped) {

  public static final LinkSummary NONE = new LinkSummary(0, 0);
}
