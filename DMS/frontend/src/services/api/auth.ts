import type { Me } from '../../types/auth'
import { api } from './client'
import { getMockMe, markMockPasswordChanged, mockRole } from './mock'

// API Spec mục 4.1. Khi VITE_MOCK_ROLE được đặt (chỉ ở dev), getMe trả người dùng giả.
export async function getMe(): Promise<Me> {
  if (mockRole) return getMockMe()
  return (await api.get<Me>('/auth/me')).data
}

export async function login(username: string, password: string): Promise<Me> {
  return (await api.post<Me>('/auth/login', { username, password })).data
}

export async function logout(): Promise<void> {
  if (mockRole) return
  await api.post('/auth/logout')
}

export async function changePassword(currentPassword: string, newPassword: string): Promise<void> {
  if (mockRole) return markMockPasswordChanged()
  await api.post('/auth/change-password', { currentPassword, newPassword })
}
