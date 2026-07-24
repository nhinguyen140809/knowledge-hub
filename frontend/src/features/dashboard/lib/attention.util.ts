import { ATTENTION_RULES, type AttentionInputs, type AttentionItem } from './attention.rules'

export type { AttentionInputs, AttentionItem, AttentionTone } from './attention.rules'

/**
 * The "needs attention" list: connected-but-worth-flagging conditions, most
 * actionable first. The judgement of what counts (and how loud) lives in the
 * individual rules in {@link ./attention.rules}; this only runs them in order
 * and drops the ones that don't apply.
 */
export function deriveAttentionItems(inputs: AttentionInputs): AttentionItem[] {
  return ATTENTION_RULES.map((rule) => rule(inputs)).filter((item) => item !== null)
}
