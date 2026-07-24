import { type ReactNode, useEffect } from 'react'
import { create } from 'zustand'

interface HeaderState {
  actions: ReactNode
  setActions: (actions: ReactNode) => void
}

/** Transient (non-persisted) slot for the page-specific buttons AppHeader
 *  renders next to the theme toggle. AppHeader is mounted once in AppLayout,
 *  outside the routed <Outlet/> subtree, so a page can't pass it props
 *  directly — it registers its actions here instead. */
const useHeaderStore = create<HeaderState>((set) => ({
  actions: null,
  setActions: (actions) => set({ actions }),
}))

/** Read side, used by AppHeader. */
export function useHeaderActions(): ReactNode {
  return useHeaderStore((s) => s.actions)
}

/** Write side: a page calls this with its action buttons and they appear in
 *  the header for as long as the page stays mounted. */
export function useSetHeaderActions(actions: ReactNode) {
  const setActions = useHeaderStore((s) => s.setActions)
  useEffect(() => {
    setActions(actions)
    return () => setActions(null)
  }, [actions, setActions])
}
