import {
  useCallback,
  useEffect,
  useLayoutEffect,
  useRef,
  useState,
  type ComponentPropsWithoutRef,
  type ReactNode,
} from 'react'
import { createPortal } from 'react-dom'
import { Link } from 'react-router'
import { Button } from './Surface'

/** Một dòng trong menu thao tác: hành động, liên kết, nhãn nhóm hoặc đường phân cách. */
export interface ActionMenuItem {
  key: string
  label?: ReactNode
  icon?: ReactNode
  danger?: boolean
  disabled?: boolean
  /** Đường dẫn nội bộ: render liên kết thay vì nút. */
  to?: string
  /** Chỉ hiện nhãn nhóm, không bấm được. */
  group?: boolean
  /** Đường kẻ phân cách giữa các nhóm mục. */
  divider?: boolean
  onClick?: () => void
}

type TriggerProps = Omit<ComponentPropsWithoutRef<typeof Button>, 'onClick'>

interface Props extends TriggerProps {
  items: ActionMenuItem[]
  /** Nội dung nút mở menu (biểu tượng hoặc avatar). */
  children?: ReactNode
}

const MENU_GAP = 4
const VIEWPORT_MARGIN = 8

/**
 * Menu thao tác dùng chung thay cho Dropdown của antd ở các nút ba chấm và avatar.
 * Menu vẽ qua portal ở body với vị trí cố định theo nút, nên không bị bảng hoặc khung có
 * overflow cắt; đóng khi bấm ra ngoài, nhấn Escape, cuộn hoặc đổi kích thước cửa sổ.
 */
export default function ActionMenu({ items, children, ...triggerProps }: Props) {
  const [open, setOpen] = useState(false)
  const [position, setPosition] = useState({ top: 0, left: 0 })
  const triggerRef = useRef<HTMLButtonElement | HTMLAnchorElement>(null)
  const menuRef = useRef<HTMLDivElement>(null)

  const place = useCallback(() => {
    const trigger = triggerRef.current?.getBoundingClientRect()
    const menu = menuRef.current
    if (!trigger || !menu) return
    const width = menu.offsetWidth
    const height = menu.offsetHeight
    const left = Math.max(
      VIEWPORT_MARGIN,
      Math.min(trigger.right - width, window.innerWidth - width - VIEWPORT_MARGIN),
    )
    const fitsBelow = trigger.bottom + MENU_GAP + height <= window.innerHeight - VIEWPORT_MARGIN
    const top = fitsBelow
      ? trigger.bottom + MENU_GAP
      : Math.max(VIEWPORT_MARGIN, trigger.top - MENU_GAP - height)
    setPosition({ top, left })
  }, [])

  useLayoutEffect(() => {
    if (open) place()
  }, [open, place])

  useEffect(() => {
    if (!open) return undefined
    const close = () => setOpen(false)
    const onPointerDown = (event: MouseEvent) => {
      const target = event.target as Node
      if (menuRef.current?.contains(target) || triggerRef.current?.contains(target)) return
      setOpen(false)
    }
    const onKeyDown = (event: KeyboardEvent) => {
      if (event.key === 'Escape') setOpen(false)
    }
    document.addEventListener('mousedown', onPointerDown)
    document.addEventListener('keydown', onKeyDown)
    window.addEventListener('resize', close)
    window.addEventListener('scroll', close, true)
    return () => {
      document.removeEventListener('mousedown', onPointerDown)
      document.removeEventListener('keydown', onKeyDown)
      window.removeEventListener('resize', close)
      window.removeEventListener('scroll', close, true)
    }
  }, [open])

  const renderItem = (item: ActionMenuItem) => {
    if (item.divider) {
      return <div key={item.key} className="dms-action-menu-divider" role="separator" />
    }
    if (item.group) {
      return (
        <div key={item.key} className="dms-action-menu-group">
          {item.label}
        </div>
      )
    }
    const className = `dms-action-menu-item${item.danger ? ' is-danger' : ''}`
    const content = (
      <>
        {item.icon && <span className="dms-action-menu-icon">{item.icon}</span>}
        <span>{item.label}</span>
      </>
    )
    if (item.to) {
      return (
        <Link
          key={item.key}
          to={item.to}
          role="menuitem"
          className={className}
          onClick={() => setOpen(false)}
        >
          {content}
        </Link>
      )
    }
    return (
      <button
        key={item.key}
        type="button"
        role="menuitem"
        className={className}
        disabled={item.disabled}
        onClick={() => {
          setOpen(false)
          item.onClick?.()
        }}
      >
        {content}
      </button>
    )
  }

  return (
    <>
      <Button
        {...triggerProps}
        ref={triggerRef}
        aria-haspopup="menu"
        aria-expanded={open}
        onClick={() => setOpen((current) => !current)}
      >
        {children}
      </Button>
      {open &&
        createPortal(
          <div
            ref={menuRef}
            role="menu"
            className="dms-action-menu"
            style={{ top: position.top, left: position.left }}
          >
            {items.map(renderItem)}
          </div>,
          document.body,
        )}
    </>
  )
}
