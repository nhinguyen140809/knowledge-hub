import { useReducer } from 'react'

type FieldAction<T> =
  { type: 'set'; key: keyof T; value: T[keyof T] } | { type: 'replace'; value: T }

function fieldReducer<T>(state: T, action: FieldAction<T>): T {
  switch (action.type) {
    case 'set':
      return { ...state, [action.key]: action.value }
    case 'replace':
      return action.value
  }
}

/**
 * State for a flat form object: one `setField(key)` updater for every field
 * regardless of its value type (string, string[], enum, ...), plus `replace`
 * to reset/reinitialize the whole object in a single dispatch. Prefer this
 * over separate useState calls per field once a form has enough fields that
 * tracking which setter goes with which key becomes the hard part.
 */
export function useFormReducer<T extends object>(initial: T | (() => T)) {
  const [state, dispatch] = useReducer(fieldReducer<T>, initial, (arg) =>
    typeof arg === 'function' ? (arg as () => T)() : arg,
  )

  const setField =
    <K extends keyof T>(key: K) =>
    (value: T[K]) =>
      dispatch({ type: 'set', key, value })

  const replace = (value: T) => dispatch({ type: 'replace', value })

  return [state, setField, replace] as const
}
