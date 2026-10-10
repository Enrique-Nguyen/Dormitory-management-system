import { useSyncExternalStore } from 'react'

// Breakpoint theo UI/UX mục 25: 360–639 mobile, 640–1023 tablet, 1024+ desktop.
export const BREAKPOINT_TABLET = 640
export const BREAKPOINT_DESKTOP = 1024

export type Viewport = 'mobile' | 'tablet' | 'desktop'

function subscribe(callback: () => void) {
  window.addEventListener('resize', callback)
  return () => window.removeEventListener('resize', callback)
}

function getViewport(): Viewport {
  const width = window.innerWidth
  if (width >= BREAKPOINT_DESKTOP) return 'desktop'
  if (width >= BREAKPOINT_TABLET) return 'tablet'
  return 'mobile'
}

export function useViewport(): Viewport {
  return useSyncExternalStore(subscribe, getViewport, () => 'desktop')
}

export function useIsMobile(): boolean {
  return useViewport() === 'mobile'
}
