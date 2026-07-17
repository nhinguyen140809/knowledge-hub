/**
 * Backend response shapes. Successful responses are returned unwrapped — a
 * resource or a raw array, keyed off the 2xx status — so there is no success
 * envelope to model. Errors are the one standardised shape: RFC 7807
 * ProblemDetail, produced by the server's GlobalExceptionHandler.
 */

/** Stable error codes from the server's ErrorCode enum. These names are a
 *  contract clients branch on; renaming one is a breaking change server-side. */
export type ApiErrorCode =
  | 'SOURCE_NOT_FOUND'
  | 'DUPLICATE_SOURCE'
  | 'PRINCIPAL_NOT_FOUND'
  | 'DUPLICATE_PRINCIPAL'
  | 'DUPLICATE_CREDENTIAL_NAME'
  | 'VALIDATION_FAILED'
  | 'UNAUTHENTICATED'
  | 'FORBIDDEN'
  | 'EMBEDDING_PROVIDER_UNAVAILABLE'
  | 'INTERNAL_ERROR'

/**
 * RFC 7807 error body: { type, title, status, detail, code, traceId }. `code` is
 * the app's ErrorCode; the union preserves autocomplete while `(string & {})`
 * still allows codes added on the server before this list catches up.
 */
export interface ProblemDetail {
  type: string
  title: string
  status: number
  detail: string
  code: ApiErrorCode | (string & {})
  traceId?: string
  instance?: string
}
