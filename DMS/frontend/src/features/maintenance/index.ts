// Feature maintenance (TV4): màn chi tiết ticket, modal tạo/thao tác và hook dữ liệu.
// Hàm gọi API của module đặt ở services/api/maintenance.ts.
export { default as MaintenanceDetailView } from './MaintenanceDetailView'
export { default as MaintenancePhotoGallery } from './MaintenancePhotoGallery'
export { default as CreateTicketModal } from './CreateTicketModal'
export { default as AssignTechnicianModal } from './AssignTechnicianModal'
export { default as TicketNoteModal } from './TicketNoteModal'
export { default as ConcludeTicketModal } from './ConcludeTicketModal'

export * from './constants'
export * from './useMaintenance'
