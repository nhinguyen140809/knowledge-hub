package com.knowledgehub.shared.pipeline;

/**
 * A single filter in a pipes-&-filters pipeline: one step, one responsibility.
 *
 * <p>A stage takes the pipeline context in and returns the (same or new) context for the next
 * stage. Implementations must be <strong>stateless across runs</strong> — all per-run state flows
 * through the context, never instance fields — and depend only on the domain ports they need, never
 * on another stage. This is what lets stages be reordered, reused (e.g. by sync), run in parallel,
 * and unit-tested in isolation.
 *
 * @param <C> the pipeline context type carried between stages
 */
@FunctionalInterface
public interface Stage<C> {

  /**
   * Applies this filter's single transformation to the context.
   *
   * @param context the incoming pipeline context
   * @return the context to hand to the next stage
   */
  C apply(C context);
}
