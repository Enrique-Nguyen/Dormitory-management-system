import { api } from './client'
import { mockRole } from './mock'

// API Spec: GET /api/notifications/unread-count → { count }
export async function getUnreadCount(): Promise<number> {
  if (mockRole) return 3
  return (await api.get<{ count: number }>('/notifications/unread-count')).data.count
}
