// Component dùng chung (UI 5.1, 27.3). Component chỉ nhận props; gọi API ở features/.
export { default as ActionBar, type ActionDef } from './ui/ActionBar'
export {
  default as ConfirmDialog,
  type ConfirmOptions,
} from './ui/ConfirmDialog'
export { useConfirm } from './ui/useConfirm'
export { default as DerivedChip } from './ui/DerivedChip'
export { default as EmptyState } from './ui/EmptyState'
export { default as FormField, type FieldControlProps } from './ui/FormField'
export { default as PageSkeleton } from './ui/PageSkeleton'
export { default as ResponsiveTable } from './ui/ResponsiveTable'
export { default as DataTable, type DataTableProps } from './ui/DataTable'
export {
  Input,
  PasswordInput,
  TextArea,
  SearchInput,
  InputNumber,
  Select,
  Switch,
  Checkbox,
  Radio,
  RadioGroup,
  DatePicker,
  RangePicker,
} from './ui/FormControls'
export {
  Button,
  Card,
  Modal,
  Drawer,
  Tag,
  Badge,
  Tabs,
  Descriptions,
  Alert,
  Empty,
  Skeleton,
  FilterBar,
  FormGrid,
} from './ui/Surface'
export { default as StatusBadge } from './ui/StatusBadge'
export { default as StatusTimeline } from './ui/StatusTimeline'
export { stepsFromFlow, type TimelineStep } from './ui/timelineSteps'

export { default as BedCard } from './business/BedCard'
export { default as BedPicker, type PickerOption } from './business/BedPicker'
export {
  default as LeaderDialog,
  type LeaderCandidate,
} from './business/LeaderDialog'
export { default as ContractStatus } from './business/ContractStatus'
export { default as InvoiceStatus } from './business/InvoiceStatus'
export { default as NotificationItem } from './business/NotificationItem'
export { default as TransferSummary } from './business/TransferSummary'

export { default as PageHeader } from './layout/PageHeader'
