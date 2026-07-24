/** Shown wherever a value is simply absent (no ref, never used, empty list).
 *  One constant so every "nothing here" cell reads the same across the app. */
export const NO_VALUE = 'N/A'

/** Joins the parts of a one-line summary ("12 sources | 9 Git | 3 filesystem").
 *  A bar rather than a comma: the parts are facets of one thing, not a flat
 *  list of separate items. Padded so callers can just interpolate it. */
export const SUMMARY_SEP = ' | '
