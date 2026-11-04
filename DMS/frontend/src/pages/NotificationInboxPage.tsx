import { CheckOutlined, DownloadOutlined, ReloadOutlined } from '@ant-design/icons'
import { App } from 'antd'
import { useEffect, useState } from 'react'
import { useSearchParams } from 'react-router'
import {
  Alert,
  Button,
  Card,
  DataTable,
  EmptyState,
  FilterBar,
  Input,
  NotificationItem,
  PageHeader,
  SearchInput,
  Select,
} from '../components'
import {
  useInbox,
  useMarkAllRead,
  useMarkRead,
  useUnreadCount,
} from '../features/notification'
import { downloadWorkbook, EXCEL_MAX_EXPORT_ROWS } from '../features/crud/excel'
import { excelUiLabels, resources } from '../features/crud/resources'
import { useIsMobile } from '../hooks/useViewport'
import {
  searchNotifications,
  type NotificationResponse,
  type NotificationSearchQuery,
} from '../services/api/notification'
import { errorMessage } from '../utils/errorMessages'

type ReadFilter = 'all' | 'unread' | 'read'

const PAGE_SIZE = 10

const readFilterOptions: { value: ReadFilter; label: string }[] = [
  { value: 'all', label: 'Tất cả thông báo' },
  { value: 'unread', label: 'Chưa đọc' },
  { value: 'read', label: 'Đã đọc' },
]

/**
 * Hộp thư thông báo dùng chung cho bốn cổng (UI mục 24): lọc, tìm kiếm và phân trang tại API.
 * Bấm thông báo chưa đọc sẽ đánh dấu đã đọc; thông báo có link do backend suy ra thì điều hướng
 * theo link. Sau mỗi thao tác, danh sách và số chưa đọc trên chuông được làm mới.
 */
export default function NotificationInboxPage() {
  const { message } = App.useApp()
  const config = resources.notificationInbox
  const [exporting, setExporting] = useState(false)
  const isMobile = useIsMobile()
  const [searchParams, setSearchParams] = useSearchParams()
  const from = searchParams.get('from') ?? ''
  const to = searchParams.get('to') ?? ''
  const [draftFrom, setDraftFrom] = useState(from)
  const [draftTo, setDraftTo] = useState(to)

  useEffect(() => {
    setDraftFrom(from)
    setDraftTo(to)
  }, [from, to])

  const q = searchParams.get('q')?.trim() ?? ''
  const unreadParam = searchParams.get('unread')
  const readFilter: ReadFilter =
    unreadParam === 'true'
      ? 'unread'
      : unreadParam === 'false'
        ? 'read'
        : 'all'
  const pageParam = Number(searchParams.get('page') ?? '1')
  const page = Number.isSafeInteger(pageParam) && pageParam > 0 ? pageParam : 1

  const query: NotificationSearchQuery = {
    page: page - 1,
    size: PAGE_SIZE,
    q: q || undefined,
    sort: ['createdAt,desc'],
    unread: readFilter === 'all' ? undefined : readFilter === 'unread',
    from: from && !Number.isNaN(Date.parse(from)) ? new Date(from).toISOString() : undefined,
    to: to && !Number.isNaN(Date.parse(to)) ? new Date(to).toISOString() : undefined,
  }
  const inbox = useInbox(query)

  async function exportNotifications() {
    setExporting(true)
    try {
      const rows: NotificationResponse[] = []
      // Giữ cả unread=false và thứ tự của hộp thư trên mọi trang xuất.
      for (let exportPage = 0; ; exportPage++) {
        const result = await searchNotifications({
          ...query,
          page: exportPage,
          size: 100,
        })
        if (
          result.totalElements > EXCEL_MAX_EXPORT_ROWS ||
          rows.length + result.content.length > EXCEL_MAX_EXPORT_ROWS
        ) {
          void message.warning(excelUiLabels.tooManyExportRows)
          return
        }
        rows.push(...result.content)
        if (!result.content.length || rows.length >= result.totalElements) break
      }
      if (!rows.length) {
        void message.info(excelUiLabels.exportEmpty)
        return
      }
      await downloadWorkbook(
        config.title,
        config.columns,
        rows.map((row) => ({ ...row })),
      )
      void message.success(excelUiLabels.exportSuccess)
    } catch (error) {
      void message.error(errorMessage(error))
    } finally {
      setExporting(false)
    }
  }
  const unreadCount = useUnreadCount()
  const unread = unreadCount.data ?? 0
  const markRead = useMarkRead()
  const markAllRead = useMarkAllRead()

  // Đọc thông báo cuối trang có thể làm trang hiện tại vượt quá tổng số trang mới.
  useEffect(() => {
    if (inbox.isFetching || inbox.error || !inbox.data) return
    const lastPage = Math.max(1, inbox.data.totalPages)
    if (page <= lastPage) return
    const next = new URLSearchParams(searchParams)
    if (lastPage > 1) next.set('page', String(lastPage))
    else next.delete('page')
    setSearchParams(next, { replace: true })
  }, [
    inbox.data,
    inbox.error,
    inbox.isFetching,
    page,
    searchParams,
    setSearchParams,
  ])

  /** Đổi bộ lọc/tìm kiếm luôn quay về trang đầu; chỉ tham số trang giữ giá trị mới. */
  function updateQuery(patch: {
    q?: string
    unread?: ReadFilter
    page?: number
    from?: string
    to?: string
  }) {
    const next = new URLSearchParams(searchParams)
    if (patch.q !== undefined) {
      const keyword = patch.q.trim()
      if (keyword) next.set('q', keyword)
      else next.delete('q')
    }
    if (patch.unread !== undefined) {
      if (patch.unread === 'all') next.delete('unread')
      else next.set('unread', patch.unread === 'unread' ? 'true' : 'false')
    }
    for (const key of ['from', 'to'] as const) {
      if (patch[key] !== undefined) {
        if (patch[key]) next.set(key, patch[key])
        else next.delete(key)
      }
    }
    if ((patch.page ?? 1) > 1) next.set('page', String(patch.page))
    else next.delete('page')
    setSearchParams(next, { replace: true })
  }

  function markAllAsRead() {
    markAllRead.mutate(undefined, {
      onSuccess: () => {
        void message.success('Đã đánh dấu tất cả thông báo là đã đọc.')
      },
    })
  }

  const unreadSubtitle = unreadCount.isPending
    ? 'Đang tải số thông báo chưa đọc'
    : unreadCount.isError
      ? 'Không tải được số thông báo chưa đọc'
      : unread > 0
        ? `${unread} thông báo chưa đọc`
        : 'Tất cả thông báo đã được đọc'

  const emptyTitle = q
    ? 'Không tìm thấy thông báo phù hợp'
    : readFilter === 'unread'
      ? 'Bạn đã đọc hết thông báo'
      : readFilter === 'read'
        ? 'Chưa có thông báo nào được đọc'
        : 'Hộp thư đang trống'
  const emptyDescription = q
    ? 'Thử từ khóa khác hoặc xóa từ khóa tìm kiếm.'
    : readFilter === 'unread'
      ? 'Thông báo mới sẽ xuất hiện tại đây.'
      : readFilter === 'read'
        ? 'Thông báo đã đọc sẽ xuất hiện tại đây.'
        : 'Thông báo hệ thống và thông báo từ đơn vị sẽ xuất hiện tại đây.'

  return (
    <>
      <PageHeader
        title="Hộp thư thông báo"
        subtitle={unreadSubtitle}
        extra={
          <Button
            icon={<CheckOutlined />}
            loading={markAllRead.isPending}
            disabled={
              unreadCount.isPending || unreadCount.isError || unread === 0
            }
            onClick={markAllAsRead}
            style={isMobile ? { width: '100%' } : undefined}
          >
            Đọc tất cả
          </Button>
        }
      />
      <Card>
        <div className="dms-toolbar">
          <FilterBar>
            <SearchInput
              key={`inbox:${q}`}
              defaultValue={q}
              maxLength={500}
              placeholder="Tìm theo tiêu đề hoặc nội dung"
              enterButton="Tìm"
              onSearch={(value) => updateQuery({ q: value })}
              onChange={(event) => {
                if (!event.target.value && q) updateQuery({ q: '' })
              }}
            />
            <Select<ReadFilter>
              aria-label="Lọc theo trạng thái đọc"
              value={readFilter}
              options={readFilterOptions}
              onChange={(value) => updateQuery({ unread: value })}
            />
            <Input
              type="datetime-local"
              aria-label="Thời gian tạo từ"
              value={draftFrom}
              onChange={(event) => setDraftFrom(event.target.value)}
            />
            <Input
              type="datetime-local"
              aria-label="Thời gian tạo đến"
              value={draftTo}
              onChange={(event) => setDraftTo(event.target.value)}
            />
            <Button onClick={() => {
              if (draftFrom && draftTo && new Date(draftFrom).getTime() > new Date(draftTo).getTime()) {
                void message.error('Thời gian bắt đầu phải trước hoặc bằng thời gian kết thúc.')
                return
              }
              updateQuery({ from: draftFrom, to: draftTo })
            }}>Áp dụng thời gian</Button>
            <Button onClick={() => {
              setDraftFrom('')
              setDraftTo('')
              updateQuery({ from: '', to: '' })
            }}>Xóa thời gian</Button>
            <Button
              icon={<DownloadOutlined />}
              loading={exporting}
              onClick={() => void exportNotifications()}
            >
              {excelUiLabels.export}
            </Button>
          </FilterBar>
        </div>
        <Alert
          type="info"
          showIcon
          style={{ marginBottom: 16 }}
          title={excelUiLabels.readonlyHint}
          description={config.note}
        />
        {inbox.error ? (
          <Alert
            type="error"
            showIcon
            title="Không tải được hộp thư thông báo"
            description={errorMessage(inbox.error)}
            action={
              <Button
                icon={<ReloadOutlined />}
                onClick={() => {
                  void inbox.refetch()
                }}
              >
                Thử lại
              </Button>
            }
          />
        ) : (
          <DataTable<NotificationResponse>
            showHeader={false}
            showStt={false}
            loading={inbox.isFetching}
            rowKey="id"
            dataSource={inbox.data?.content ?? []}
            locale={{
              emptyText: (
                <EmptyState
                  title={emptyTitle}
                  description={emptyDescription}
                />
              ),
            }}
            columns={[
              {
                key: 'notification',
                width: '100%',
                render: (_value: unknown, item: NotificationResponse) => (
                  <NotificationItem
                    title={item.title}
                    body={item.body}
                    createdAt={item.createdAt}
                    isRead={item.isRead}
                    link={item.link}
                    onClick={
                      item.isRead
                        ? undefined
                        : () => markRead.mutate(item.id)
                    }
                  />
                ),
              },
            ]}
            pagination={
              inbox.data && inbox.data.totalElements > 0
                ? {
                    current: page,
                    pageSize: PAGE_SIZE,
                    total: inbox.data.totalElements,
                    showSizeChanger: false,
                    simple: isMobile,
                    showTotal: (total) => `Tổng ${total} thông báo`,
                    onChange: (nextPage) => updateQuery({ page: nextPage }),
                  }
                : false
            }
          />
        )}
      </Card>
    </>
  )
}
