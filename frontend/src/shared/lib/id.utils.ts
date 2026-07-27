/**
 * The id charset shared by every user-coined identifier that ends up in a URL
 * path (a source id, a principal id): lowercase letters, digits, and single
 * hyphens between them. One case only, so `Alice` and `alice` can't become two
 * distinct principals, and a restricted set stays readable and safe in a path.
 */
export const ID_PATTERN = /^[a-z0-9]+(-[a-z0-9]+)*$/

/**
 * Validator for HeroUI's `validate` prop: null when the value is a well-formed
 * id, otherwise a message. `example` tailors the hint to the field. Client-side
 * feedback only; the server re-validates the same shape on submit.
 */
export function validateId(value: string, example = 'engineering-wiki'): string | null {
  return ID_PATTERN.test(value)
    ? null
    : `Lowercase letters, numbers and hyphens only (e.g. ${example})`
}
