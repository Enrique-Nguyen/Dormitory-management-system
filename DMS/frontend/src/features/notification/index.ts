// Feature notification (TV4, TV5): hook TanStack Query cho hộp thư và số chưa đọc.
// Hàm gọi API của module đặt ở services/api/notification.ts.
export { unreadCountKey, useUnreadCount } from './useUnreadCount'
export { inboxKeys, useInbox, useMarkAllRead, useMarkRead } from './useInbox'
