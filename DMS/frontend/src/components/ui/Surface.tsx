import {
  Alert as AntAlert,
  Badge as AntBadge,
  Button as AntButton,
  Card as AntCard,
  Descriptions as AntDescriptions,
  Drawer as AntDrawer,
  Empty as AntEmpty,
  Modal as AntModal,
  Popover,
  Skeleton as AntSkeleton,
  Tabs as AntTabs,
  Tag as AntTag,
} from 'antd'
import {
  CloseCircleOutlined,
  DownOutlined,
  FileTextOutlined,
  FilterOutlined,
  LoadingOutlined,
  QuestionCircleOutlined,
} from '@ant-design/icons'
import {
  forwardRef,
  useId,
  useImperativeHandle,
  useState,
  type ComponentPropsWithRef,
  type HTMLAttributes,
  type ReactNode,
} from 'react'

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

export interface FormSectionHandle {
  open: () => void
  close: () => void
  toggle: () => void
}

export interface FormSectionProps {
  title: ReactNode
  defaultOpen?: boolean
  sectionId?: string
  extra?: ReactNode
  icon?: ReactNode
  description?: ReactNode
  className?: string
  collapsible?: boolean
  children?: ReactNode
}

/**
 * FormSection (collapsible panel) theo chuẩn Parkway HRM V5:
 * - Khi mở (open): Nền bg-accent-bg/60 text-accent (xanh nhạt chữ màu nhấn), icon ChevronDown quay xuống.
 * - Khi đóng: Nền bg-bg-warm/40 text-text, icon ChevronDown xoay ngang (-rotate-90).
 * - Click vào thanh tiêu đề để đóng/mở nội dung bên trong.
 */
export const FormSection = forwardRef<FormSectionHandle, FormSectionProps>(
  function FormSection(
    {
      title,
      defaultOpen = true,
      sectionId,
      extra,
      icon,
      description,
      className = '',
      collapsible = true,
      children,
    },
    ref,
  ) {
    const [open, setOpen] = useState(defaultOpen)

    useImperativeHandle(ref, () => ({
      open: () => setOpen(true),
      close: () => setOpen(false),
      toggle: () => setOpen((prev) => !prev),
    }))

    return (
      <div
        className={`dms-form-section ${open ? 'dms-form-section-open' : 'dms-form-section-collapsed'} ${className}`}
        data-section-id={sectionId}
      >
        {collapsible ? (
          <button
            type="button"
            onClick={() => setOpen(!open)}
            aria-expanded={open}
            className={`dms-form-section-header ${open ? 'dms-header-open' : 'dms-header-closed'}`}
          >
            <span className="dms-form-section-title-wrap">
              <DownOutlined
                className={`dms-form-section-chevron ${open ? 'rotate-open' : 'rotate-closed'}`}
              />
              {icon && <span className="dms-form-section-icon">{icon}</span>}
              <span className="dms-form-section-title">{title}</span>
            </span>
            {extra && (
              <span
                className="dms-form-section-extra"
                onClick={(e) => e.stopPropagation()}
              >
                {extra}
              </span>
            )}
          </button>
        ) : (
          <div className="dms-form-section-header dms-header-open">
            <span className="dms-form-section-title-wrap">
              {icon && <span className="dms-form-section-icon">{icon}</span>}
              <span className="dms-form-section-title">{title}</span>
            </span>
            {extra && <span className="dms-form-section-extra">{extra}</span>}
          </div>
        )}
        {description && open && (
          <div className="dms-form-section-desc">{description}</div>
        )}
        {open && <div className="dms-form-section-body">{children}</div>}
      </div>
    )
  },
)
FormSection.displayName = 'FormSection'

export function DetailSection({
  title,
  icon = <FileTextOutlined />,
  description,
  className = '',
  collapsible = true,
  defaultOpen = true,
  extra,
  children,
  ...props
}: Omit<ComponentPropsWithRef<typeof AntCard>, 'title'> & {
  title?: ReactNode
  icon?: ReactNode
  description?: ReactNode
  collapsible?: boolean
  defaultOpen?: boolean
}) {
  if (title) {
    return (
      <FormSection
        title={title}
        icon={icon}
        description={description}
        extra={extra}
        collapsible={collapsible}
        defaultOpen={defaultOpen}
        className={`dms-detail-section ${className}`}
      >
        {children}
      </FormSection>
    )
  }
  return (
    <Card {...props} className={`dms-detail-section ${className}`}>
      {children}
    </Card>
  )
}

export interface ModalProps extends ComponentPropsWithRef<typeof AntModal> {
  headerIcon?: ReactNode
  subtitle?: ReactNode
}

/** Modal dùng chung chuẩn V5; hỗ trợ icon vuông bo góc nền bg-accent-bg text-accent và subtitle bên cạnh title. */
export function Modal({
  className = '',
  title,
  headerIcon,
  subtitle,
  width = 960,
  ...props
}: ModalProps) {
  const renderedTitle =
    headerIcon || subtitle ? (
      <div className="dms-modal-header-v5">
        {headerIcon && (
          <span className="dms-modal-header-icon">{headerIcon}</span>
        )}
        <div className="dms-modal-header-text">
          <div className="dms-modal-header-title">{title}</div>
          {subtitle && (
            <div className="dms-modal-header-subtitle">{subtitle}</div>
          )}
        </div>
      </div>
    ) : (
      title
    )

  return (
    <AntModal
      width={width}
      style={{ top: 24 }}
      title={renderedTitle}
      {...props}
      className={`dms-modal ${className}`}
    />
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
  return (
    <AntDescriptions
      layout="vertical"
      colon={false}
      {...props}
      className={`dms-descriptions ${className}`}
    />
  )
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

export interface ListToolbarProps {
  leadingControls?: ReactNode
  help?: ReactNode
  helpLabel?: string
  search: ReactNode
  actions?: ReactNode
  /** Nút hành động chính (+ Thêm ...) nổi bật ở ngoài cùng bên phải */
  primaryAction?: ReactNode
  /** Danh sách thao tác phụ gom vào menu ba chấm "..." */
  moreActionItems?: Array<{
    key: string
    label: ReactNode
    icon?: ReactNode
    danger?: boolean
    disabled?: boolean
    onClick?: () => void
  }>
  /** Số lượng dòng được chọn để thao tác hàng loạt */
  selectedCount?: number
  /** Hàm xử lý xóa nhiều dòng đã chọn */
  onBulkDelete?: () => void
  bulkDeleteLabel?: string
  filters?: ReactNode
  filterCount?: number
  filterLabel?: string
  closeFiltersLabel?: string
  filtersOpen?: boolean
  onFiltersOpenChange?: (open: boolean) => void
}

export function ListToolbar({
  leadingControls,
  search,
  actions,
  primaryAction,
  moreActionItems: _moreActionItems,
  selectedCount = 0,
  onBulkDelete,
  bulkDeleteLabel = 'Xóa đã chọn',
  filters,
  filterCount = 0,
  filterLabel = 'Bộ lọc',
  closeFiltersLabel = 'Đóng bộ lọc',
  help,
  helpLabel,
  filtersOpen: controlledFiltersOpen,
  onFiltersOpenChange,
}: ListToolbarProps) {
  const [uncontrolledFiltersOpen, setUncontrolledFiltersOpen] = useState(filterCount > 0)
  const isFiltersOpen = controlledFiltersOpen !== undefined ? controlledFiltersOpen : uncontrolledFiltersOpen
  const setFiltersOpen = (open: boolean) => {
    if (controlledFiltersOpen === undefined) setUncontrolledFiltersOpen(open)
    onFiltersOpenChange?.(open)
  }
  const filtersId = useId()

  return (
    <div className="dms-list-toolbar">
      <div className="dms-list-toolbar-main">
        <div className="dms-list-toolbar-left">
          {leadingControls}
          <div className="dms-list-toolbar-search">{search}</div>
          {filters && (
            <Button
              icon={<FilterOutlined />}
              aria-expanded={isFiltersOpen}
              aria-controls={filtersId}
              className={`dms-toolbar-filter-btn ${isFiltersOpen || filterCount > 0 ? 'dms-filter-btn-active' : ''}`}
              onClick={() => setFiltersOpen(!isFiltersOpen)}
            >
              <span>{isFiltersOpen ? closeFiltersLabel : filterLabel}</span>
              {filterCount > 0 && <span className="dms-filter-count">{filterCount}</span>}
            </Button>
          )}
          {help && (
            <Popover
              content={<div className="dms-list-help-content">{help}</div>}
              trigger={['hover', 'focus', 'click']}
              placement="bottomLeft"
            >
              <Button type="link" icon={<QuestionCircleOutlined />} className="dms-toolbar-help-btn">
                {helpLabel}
              </Button>
            </Popover>
          )}
        </div>

        <div className="dms-list-toolbar-actions">
          {selectedCount > 0 && onBulkDelete && (
            <Button
              danger
              icon={<CloseCircleOutlined />}
              onClick={onBulkDelete}
              className="dms-bulk-delete-btn"
            >
              {bulkDeleteLabel} ({selectedCount})
            </Button>
          )}

          {actions}

          {primaryAction}
        </div>
      </div>
      {filters && (
        <div id={filtersId} className="dms-list-toolbar-filters" hidden={!isFiltersOpen}>
          {filters}
        </div>
      )}
    </div>
  )
}

export interface StatusFilterOption {
  value: string
  label: ReactNode
  count?: number
  loading?: boolean
  variant?: 'default' | 'warning' | 'success' | 'danger' | 'info'
  icon?: ReactNode
}

/**
 * Status Tabs ngang chuẩn Parkway HRM V5:
 * Clean tabs ngang có line gạch chân active và badge count pill,
 * cuộn mượt mà trên mobile/desktop không vỡ layout.
 */
export function StatusFilter({
  label,
  value,
  options,
  onChange,
}: {
  label: string
  value: string
  options: StatusFilterOption[]
  onChange: (value: string) => void
}) {
  return (
    <div className="dms-status-tabs-wrapper" role="group" aria-label={label}>
      <div className="dms-status-tabs-list">
        {options.map((option) => {
          const isActive = value === option.value
          const textLabel = typeof option.label === 'string' ? option.label : ''
          const displayLabel = option.value === '' && (textLabel === 'Tất cả' || !textLabel) ? 'Tất cả' : option.label

          return (
            <button
              key={option.value}
              type="button"
              aria-pressed={isActive}
              className={`dms-status-tab-item ${isActive ? 'dms-status-tab-active' : ''}`}
              onClick={() => onChange(option.value)}
            >
              <span className="dms-status-tab-label">{displayLabel}</span>
              {(option.count !== undefined || option.loading) && (
                <span className="dms-status-tab-count">
                  {option.loading ? <LoadingOutlined spin /> : (option.count ?? 0)}
                </span>
              )}
            </button>
          )
        })}
      </div>
    </div>
  )
}

export function DetailLayout({
  className = '',
  ...props
}: HTMLAttributes<HTMLDivElement>) {
  return <div {...props} className={`dms-detail-layout ${className}`} />
}

/** Trường dài đặt class dms-form-span-full; mobile luôn hiển thị một cột. */
export function FormGrid({
  className = '',
  ...props
}: HTMLAttributes<HTMLDivElement>) {
  return <div {...props} className={`dms-form-grid ${className}`} />
}
