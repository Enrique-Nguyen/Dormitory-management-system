import { lazy, Suspense, type ComponentType } from 'react'
import { PageSkeleton } from '../../components'

/**
 * Tách mỗi trang thành chunk riêng, chỉ tải khi người dùng mở route; khung layout và guard vẫn tải
 * sẵn nên menu không nhấp nháy. Fallback dùng skeleton chung của trang.
 */
export function lazyPage<P extends object>(
  load: () => Promise<{ default: ComponentType<P> }>,
): ComponentType<P> {
  const Page = lazy(load)
  return function LazyPage(props: P) {
    return (
      <Suspense fallback={<PageSkeleton />}>
        <Page {...props} />
      </Suspense>
    )
  }
}
