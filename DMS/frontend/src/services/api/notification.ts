import { api } from './client'

// API Spec: GET /api/notifications/unread-count → { count }
export async function getUnreadCount(): Promise<number> {
  return (await api.get<{ count: number }>('/notifications/unread-count')).data
    .count
}
