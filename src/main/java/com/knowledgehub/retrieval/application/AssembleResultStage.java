package com.knowledgehub.retrieval.application;

import com.knowledgehub.knowledge.domain.ScoredId;
import com.knowledgehub.retrieval.domain.Hit;
import com.knowledgehub.retrieval.domain.HitMetadata;
import com.knowledgehub.retrieval.domain.RetrievalReadPort;
import com.knowledgehub.shared.pipeline.Stage;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Loads the metadata for the fused candidates and turns them into {@link Hit}s, keeping the fused
 * order. The metadata load is itself scoped to {@code allowedSources}, so this is also where the
 * ACL is re-applied after fusion: a candidate whose source is not readable has no metadata loaded
 * and is dropped here, never reaching the result.
 */
@Component
class AssembleResultStage implements Stage<RetrievalContext> {

  private final RetrievalReadPort reader;

  AssembleResultStage(RetrievalReadPort reader) {
    this.reader = reader;
  }

  @Override
  public RetrievalContext apply(RetrievalContext context) {
    List<ScoredId> fused = context.fusedHits();
    if (fused.isEmpty()) {
      context.setAssembledHits(List.of());
      return context;
    }
    List<String> ids = fused.stream().map(ScoredId::chunkId).toList();
    Map<String, HitMetadata> metadata = reader.loadMetadata(ids, context.aclFilter());

    List<Hit> hits = new ArrayList<>(fused.size());
    for (ScoredId scored : fused) {
      HitMetadata meta = metadata.get(scored.chunkId());
      if (meta != null) {
        hits.add(new Hit(scored.chunkId(), scored.score(), meta));
      }
    }
    context.setAssembledHits(hits);
    return context;
  }
}
