import {
  Alert as AntAlert,
  Badge as AntBadge,
  Button as AntButton,
  Card as AntCard,
  Descriptions as AntDescriptions,
  Drawer as AntDrawer,
  Empty as AntEmpty,
  Modal as AntModal,
  Skeleton as AntSkeleton,
  Tabs as AntTabs,
  Tag as AntTag,
} from 'antd'
import type { ComponentPropsWithRef, HTMLAttributes } from 'react'

export function Button({
  className = '',
  ...props
}: ComponentPropsWithRef<typeof AntButton>) {
  return <AntButton {...props} className={`dms-button ${className}`} />
}

export function Card({
  className = '',
  ...props
}: ComponentPropsWithRef<typeof AntCard>) {
  return <AntCard {...props} className={`dms-panel ${className}`} />
}

/** Modal dùng chung chỉ đặt giao diện; loading, xác nhận và việc đóng do màn gọi quyết định. */
export function Modal({
  className = '',
  ...props
}: ComponentPropsWithRef<typeof AntModal>) {
  return (
    <AntModal width={640} {...props} className={`dms-modal ${className}`} />
  )
}

export function Drawer({
  className = '',
  ...props
}: ComponentPropsWithRef<typeof AntDrawer>) {
  return <AntDrawer {...props} className={`dms-drawer ${className}`} />
}

export function Tag({
  className = '',
  ...props
}: ComponentPropsWithRef<typeof AntTag>) {
  return <AntTag {...props} className={`dms-tag ${className}`} />
}

export function Badge({
  className = '',
  ...props
}: ComponentPropsWithRef<typeof AntBadge>) {
  return <AntBadge {...props} className={`dms-badge ${className}`} />
}

export function Tabs({
  className = '',
  ...props
}: ComponentPropsWithRef<typeof AntTabs>) {
  return <AntTabs {...props} className={`dms-tabs ${className}`} />
}

export function Descriptions({
  className = '',
  ...props
}: ComponentPropsWithRef<typeof AntDescriptions>) {
  return <AntDescriptions {...props} className={`dms-descriptions ${className}`} />
}

export function Alert({
  className = '',
  ...props
}: ComponentPropsWithRef<typeof AntAlert>) {
  return <AntAlert {...props} className={`dms-alert ${className}`} />
}

export function Empty({
  className = '',
  ...props
}: ComponentPropsWithRef<typeof AntEmpty>) {
  return <AntEmpty {...props} className={`dms-empty ${className}`} />
}

export function Skeleton({
  className = '',
  ...props
}: ComponentPropsWithRef<typeof AntSkeleton>) {
  return <AntSkeleton {...props} className={`dms-skeleton ${className}`} />
}

/** Toolbar co giãn theo chiều rộng màn hình, dùng chung cho tìm kiếm và bộ lọc. */
export function FilterBar({
  className = '',
  ...props
}: HTMLAttributes<HTMLDivElement>) {
  return <div {...props} className={`dms-filter-bar ${className}`} />
}

/** Trường dài đặt class dms-form-span-full; mobile luôn hiển thị một cột. */
export function FormGrid({
  className = '',
  ...props
}: HTMLAttributes<HTMLDivElement>) {
  return <div {...props} className={`dms-form-grid ${className}`} />
}
