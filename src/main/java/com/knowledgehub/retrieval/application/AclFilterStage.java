package com.knowledgehub.retrieval.application;

import com.knowledgehub.retrieval.domain.Hit;
import com.knowledgehub.retrieval.domain.RankedResult;
import com.knowledgehub.shared.pipeline.Stage;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * The final filter: applies the functional {@code ref} and {@code type} restrictions and truncates
 * to {@code topK}, then packages the {@link RankedResult}. The source ACL is already enforced
 * upstream - pushed into every search path and re-applied when metadata is loaded - so what remains
 * here is the caller's own {@code ref}/{@code type} narrowing and the top-k cut.
 */
@Component
class AclFilterStage implements Stage<RetrievalContext> {

  @Override
  public RetrievalContext apply(RetrievalContext context) {
    String ref = context.effectiveRef();
    String type = context.typeFilter();
    List<Hit> hits =
        context.assembledHits().stream()
            .filter(hit -> ref == null || ref.equals(hit.metadata().ref()))
            .filter(hit -> type == null || type.equals(hit.metadata().type()))
            .limit(context.topK())
            .toList();
    context.setResult(new RankedResult(hits, context.servedFromCanonicalRef()));
    return context;
  }
}
