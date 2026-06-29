package com.knowledgehub.shared.pipeline;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Runs an ordered list of {@link Stage} filters over a context, timing and logging each stage. The
 * generic, business-free building block for the indexing and query pipelines; concrete stages live
 * in each feature's {@code application/} package.
 *
 * @param <C> the pipeline context type carried between stages
 */
public final class Pipeline<C> {

  private static final Logger log = LoggerFactory.getLogger(Pipeline.class);

  private final List<Stage<C>> stages;

  /**
   * @param stages the ordered filters to run; copied defensively
   */
  public Pipeline(List<Stage<C>> stages) {
    this.stages = List.copyOf(stages);
  }

  /**
   * Runs every stage in order, feeding each stage's output into the next.
   *
   * @param context the initial context
   * @return the context after the final stage
   */
  public C run(C context) {
    C current = context;
    for (Stage<C> stage : stages) {
      String name = stage.getClass().getSimpleName();
      long startNanos = System.nanoTime();
      current = stage.apply(current);
      log.debug("Stage {} completed in {} ms", name, (System.nanoTime() - startNanos) / 1_000_000);
    }
    return current;
  }
}
