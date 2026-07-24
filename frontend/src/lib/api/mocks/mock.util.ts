/** Resolves after a short delay to mimic network latency in mock mode, so
 *  loading states are still exercised without a real backend. */
export function mockResolve<T>(value: T, ms = 300): Promise<T> {
  return new Promise((resolve) => setTimeout(() => resolve(value), ms))
}
