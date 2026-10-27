import { useState, type ReactNode } from 'react'
import { Col, Row, Space } from 'antd'
import { Link } from 'react-router'
import {
  ArrowRightOutlined,
  FileAddOutlined,
  LeftOutlined,
  PieChartOutlined,
  ReloadOutlined,
  RightOutlined,
  SolutionOutlined,
  WalletOutlined,
} from '@ant-design/icons'
import {
  Alert,
  Button,
  Card,
  PageHeader,
  PageSkeleton,
  StatusBadge,
} from '../../components'
import { Permission, Role } from '../../constants/authorization'
import {
  adminNav,
  permissionsForPath,
  roleHome,
  staffNav,
} from '../../constants/navigation'
import { hasAnyPermission, hasPermission } from '../../services/api/auth'
import { useMe } from '../../features/account/useMe'
import { useViewport } from '../../hooks/useViewport'
import { IssueInvoicesModal, useStaffDashboard } from '../../features/billing'
import {
  formatDateTime,
  formatMoney,
  formatNumber,
  formatPercent,
} from '../../utils/format'
import {
  getStatus,
  statusColors,
  statusConfig,
  roomOperation,
  type StatusEntity,
} from '../../constants/statusConfig'
import type { StaffDashboardSummary } from '../../services/api/billing'
import { dashboardUiLabels as chartLabels } from '../../features/crud/resources'
import { dashboardChartColors } from '../../app/theme'

/** Số lối tắt mỗi trang: 6 mục (3 cột × 2 hàng) từ tablet trở lên, 4 mục (2 × 2) trên mobile. */
const SHORTCUT_PAGE_SIZE = 6
const SHORTCUT_PAGE_SIZE_MOBILE = 4

/** Nhãn vùng nội dung nhỏ, chữ hoa giãn cách nhẹ để tạo nhịp điệu cổ điển. */
function SectionHeading({
  id,
  title,
  note,
}: {
  id: string
  title: string
  note?: string
}) {
  return (
    <div className="dms-dashboard-section-head">
      <h2 id={id} className="dms-dashboard-section-title">
        {title}
      </h2>
      {note && <span className="dms-dashboard-section-note">{note}</span>}
    </div>
  )
}

/** Một chỉ số chính; số tiền và phần trăm dùng định dạng chung của ứng dụng. */
function KpiCard({
  label,
  value,
  note,
  icon,
  money,
  percent,
  accent,
}: {
  label: string
  value: number
  note: string
  icon: ReactNode
  money?: boolean
  percent?: boolean
  accent?: boolean
}) {
  const formatted = money
    ? formatMoney(value)
    : percent
      ? formatPercent(value)
      : formatNumber(value)
  return (
    <Card
      className={
        'dms-dashboard-kpi' +
        (accent ? ' dms-dashboard-kpi--accent' : '')
      }
    >
      <div className="dms-kpi-head">
        <span className="dms-kpi-label">{label}</span>
        <span className="dms-kpi-icon" aria-hidden="true">
          {icon}
        </span>
      </div>
      <div className="dms-kpi-value">{formatted}</div>
      <div className="dms-kpi-note">{note}</div>
    </Card>
  )
}

/** Biểu đồ cơ cấu giường khai thác; giường bảo trì được thống kê riêng ở thẻ vận hành. */
function BedCapacityChart({
  capacity,
}: {
  capacity: StaffDashboardSummary['capacity']
}) {
  const total = capacity.operationalBeds
  const slices = [
    {
      label: chartLabels.occupied,
      value: capacity.occupiedBeds,
      color: dashboardChartColors.occupied,
    },
    {
      label: chartLabels.held,
      value: Math.max(
        0,
        total - capacity.occupiedBeds - capacity.availableBeds,
      ),
      color: dashboardChartColors.held,
    },
    {
      label: chartLabels.available,
      value: capacity.availableBeds,
      color: dashboardChartColors.available,
    },
  ]
  const segments = slices.map((slice) => ({
    ...slice,
    percent: total > 0 ? (slice.value / total) * 100 : 0,
  }))
  const description = segments
    .map((slice) => slice.label + ': ' + formatNumber(slice.value))
    .join(', ')

  return (
    <Card title={chartLabels.bedsTitle} className="dms-dashboard-chart-card">
      <div className="dms-dashboard-capacity">
        <svg
          className="dms-dashboard-donut"
          viewBox="0 0 120 120"
          role="img"
          aria-label={description || chartLabels.noBeds}
        >
          <title>{chartLabels.bedsTitle}</title>
          <circle
            cx="60"
            cy="60"
            r="44"
            fill="none"
            stroke={dashboardChartColors.track}
            strokeWidth="12"
          />
          {segments
            .filter((slice) => slice.percent > 0)
            .map((slice) => {
              const offset = segments
                .slice(0, segments.indexOf(slice))
                .reduce((sum, previous) => sum + previous.percent, 0)
              return (
                <circle
                  key={slice.label}
                  cx="60"
                  cy="60"
                  r="44"
                  fill="none"
                  stroke={slice.color}
                  strokeWidth="12"
                  pathLength="100"
                  strokeDasharray={slice.percent + ' ' + (100 - slice.percent)}
                  strokeDashoffset={-offset}
                  transform="rotate(-90 60 60)"
                >
                  <title>
                    {slice.label + ': ' + formatNumber(slice.value)}
                  </title>
                </circle>
              )
            })}
          <text
            x="60"
            y="58"
            textAnchor="middle"
            className="dms-dashboard-donut-value"
          >
            {formatNumber(total)}
          </text>
          <text
            x="60"
            y="72"
            textAnchor="middle"
            className="dms-dashboard-donut-caption"
          >
            {chartLabels.operational}
          </text>
        </svg>
        <div className="dms-dashboard-chart-legend">
          {segments.map((slice) => (
            <div className="dms-dashboard-chart-legend-row" key={slice.label}>
              <span className="dms-dashboard-chart-legend-label">
                <span
                  className="dms-dashboard-chart-swatch"
                  style={{ background: slice.color }}
                  aria-hidden="true"
                />
                {slice.label}
              </span>
              <span className="dms-dashboard-chart-legend-value">
                <strong>{formatNumber(slice.value)}</strong>
                <span>{formatPercent(slice.percent)}</span>
              </span>
            </div>
          ))}
          <span className="dms-dashboard-chart-note">
            {capacity.maintenanceBeds > 0
              ? chartLabels.maintenanceBeds +
                ': ' +
                formatNumber(capacity.maintenanceBeds)
              : chartLabels.noMaintenanceBeds}
          </span>
          {total === 0 && (
            <span className="dms-dashboard-chart-note">
              {chartLabels.noBeds}
            </span>
          )}
        </div>
      </div>
    </Card>
  )
}

/** Cơ cấu nợ hóa đơn; tổng tiền đã nằm ở thẻ chỉ số nên biểu đồ chỉ phân rã theo loại. */
function FinanceChart({
  finance,
}: {
  finance: StaffDashboardSummary['finance']
}) {
  const debtTotal = finance.totalUnpaidAmount
  const rows = [
    {
      label: chartLabels.roomFee,
      value: finance.unpaidRoomFeeAmount,
      color: dashboardChartColors.roomFee,
    },
    {
      label: chartLabels.utility,
      value: finance.unpaidUtilityAmount,
      color: dashboardChartColors.utility,
    },
  ]

  return (
    <Card title={chartLabels.debtsTitle} className="dms-dashboard-chart-card">
      <div className="dms-finance">
        {debtTotal > 0 ? (
          <>
            <div className="dms-finance-rows">
              {rows.map((item) => {
                const share = (item.value / debtTotal) * 100
                return (
                  <div className="dms-finance-row" key={item.label}>
                    <div className="dms-finance-row-head">
                      <span className="dms-dashboard-chart-legend-label">
                        <span
                          className="dms-dashboard-chart-swatch"
                          style={{ background: item.color }}
                          aria-hidden="true"
                        />
                        {item.label}
                      </span>
                      <strong>{formatMoney(item.value)}</strong>
                    </div>
                    <div
                      className="dms-finance-bar"
                      role="progressbar"
                      aria-label={item.label + ': ' + formatMoney(item.value)}
                      aria-valuemin={0}
                      aria-valuemax={100}
                      aria-valuenow={Math.round(share)}
                    >
                      <span
                        style={{
                          width: share + '%',
                          background: item.color,
                        }}
                      />
                    </div>
                    <span className="dms-finance-share">
                      {formatPercent(share)} {chartLabels.debtShare}
                    </span>
                  </div>
                )
              })}
            </div>
            <span className="dms-dashboard-chart-note">
              {chartLabels.debtBreakdownNote}
            </span>
          </>
        ) : (
          <span className="dms-dashboard-chart-note">
            {chartLabels.noInvoiceDebt}
          </span>
        )}
      </div>
    </Card>
  )
}

/** Phân bố trạng thái lấy từ số đếm của server; không tải danh sách rồi đếm trên trình duyệt. */
function StatusBreakdown({
  title,
  entity,
  counts,
}: {
  title: string
  entity: StatusEntity
  counts: Record<string, number>
}) {
  const codes = [
    ...new Set([...Object.keys(statusConfig[entity]), ...Object.keys(counts)]),
  ]
  const rows = codes.map((status) => ({
    status,
    count: counts[status] ?? 0,
    style: getStatus(entity, status),
  }))
  const total = Object.values(counts).reduce((sum, count) => sum + count, 0)
  const description = rows
    .filter((row) => row.count > 0)
    .map((row) => row.style.label + ': ' + formatNumber(row.count))
    .join(', ')

  return (
    <Card title={title} className="dms-dashboard-chart-card">
      <div className="dms-status-chart">
        <div className="dms-dashboard-chart-total">
          <span>{chartLabels.totalRecords}</span>
          <strong>{formatNumber(total)}</strong>
        </div>
        {total > 0 ? (
          <div
            className="dms-status-bar"
            role="img"
            aria-label={description}
          >
            {rows
              .filter((row) => row.count > 0)
              .map((row) => (
                <span
                  key={row.status}
                  className="dms-status-bar-segment"
                  style={{
                    width: (row.count / total) * 100 + '%',
                    background: statusColors[row.style.color],
                  }}
                  title={row.style.label + ': ' + formatNumber(row.count)}
                />
              ))}
          </div>
        ) : (
          <span className="dms-dashboard-chart-note">
            {chartLabels.noRecords}
          </span>
        )}
        <div className="dms-status-legend">
          {rows.map((row) => (
            <div className="dms-status-legend-row" key={row.status}>
              <StatusBadge entity={entity} status={row.status} />
              <span className="dms-status-legend-count">
                {formatNumber(row.count)}
              </span>
            </div>
          ))}
        </div>
        <span className="dms-dashboard-chart-note">
          {total === 0
            ? chartLabels.noRecords
            : chartLabels.statusHint + ' · ' + chartLabels.statusShare}
        </span>
      </div>
    </Card>
  )
}

export default function StaffHomePage() {
  const { data: me } = useMe()
  const viewport = useViewport()
  const {
    data: dashboard,
    isLoading,
    isFetching,
    error,
    refetch,
    dataUpdatedAt,
  } = useStaffDashboard()
  const [issueOpen, setIssueOpen] = useState(false)
  const [shortcutPage, setShortcutPage] = useState(0)
  const capacity = dashboard?.capacity
  const operations = dashboard?.operations
  const registration = dashboard?.registration
  const residency = dashboard?.residency
  const infrastructure = dashboard?.infrastructure
  const navigation = me?.role === Role.ADMIN ? adminNav : staffNav
  const homePath = roleHome[me?.role ?? Role.STAFF]
  const shortcuts = navigation
    .flatMap((group) => group.items)
    .filter(
      (item) =>
        item.path !== homePath &&
        hasAnyPermission(me, permissionsForPath(item.path)),
    )
  const shortcutPageSize =
    viewport === 'mobile' ? SHORTCUT_PAGE_SIZE_MOBILE : SHORTCUT_PAGE_SIZE
  const shortcutPageCount = Math.max(
    1,
    Math.ceil(shortcuts.length / shortcutPageSize),
  )
  const currentShortcutPage = Math.min(shortcutPage, shortcutPageCount - 1)
  const visibleShortcuts = shortcuts.slice(
    currentShortcutPage * shortcutPageSize,
    (currentShortcutPage + 1) * shortcutPageSize,
  )
  // Trang cuối vẫn đủ số ô để lưới luôn cao hai hàng và không giật chiều cao.
  const shortcutPlaceholders =
    shortcutPageCount > 1
      ? shortcutPageSize - visibleShortcuts.length
      : 0
  const changeShortcutPage = (direction: number) => {
    setShortcutPage(
      Math.max(
        0,
        Math.min(currentShortcutPage + direction, shortcutPageCount - 1),
      ),
    )
  }
  const targetFor = (permission: Permission) =>
    hasPermission(me, permission)
      ? shortcuts.find((item) => item.permissions.includes(permission))?.path
      : undefined

  const kpis: {
    label: string
    value: number
    note: string
    icon: ReactNode
    money?: boolean
    percent?: boolean
    accent?: boolean
  }[] = dashboard
    ? [
        {
          label: chartLabels.unpaidKpi,
          value: dashboard.finance.totalUnpaidAmount,
          money: true,
          note: chartLabels.unpaidNote,
          icon: <WalletOutlined />,
          accent: true,
        },
        {
          label: chartLabels.occupancy,
          value: dashboard.capacity.occupancyRate,
          percent: true,
          note: chartLabels.occupancyNote,
          icon: <PieChartOutlined />,
        },
        ...(residency
          ? [
              {
                label: chartLabels.activeContracts,
                value: residency.activeContracts,
                note: chartLabels.activeContractsNote,
                icon: <SolutionOutlined />,
              },
            ]
          : []),
        {
          label: chartLabels.topupKpi,
          value: dashboard.finance.totalTopupNeededAmount,
          money: true,
          note: chartLabels.topupNote,
          icon: <WalletOutlined />,
        },
      ]
    : []

  const queue = [
    {
      label: chartLabels.overdueMoveout,
      value: operations?.contractsOverdueMoveout ?? 0,
      permission: Permission.CONTRACT_READ,
    },
    {
      label: chartLabels.expiringContracts,
      value: operations?.contractsExpiringSoon ?? 0,
      permission: Permission.CONTRACT_READ,
    },
    ...(residency
      ? [
          {
            label: chartLabels.pendingActivation,
            value: residency.pendingActivation,
            permission: Permission.CONTRACT_READ,
          },
        ]
      : []),
    ...(registration
      ? [
          {
            label: chartLabels.pendingApplications,
            value: registration.pendingApplications,
            permission: Permission.ROOM_APPLICATION_READ,
          },
        ]
      : []),
    ...(residency
      ? [
          {
            label: chartLabels.pendingExtensions,
            value: residency.pendingExtensions,
            permission: Permission.CONTRACT_EXTENSION_READ,
          },
          {
            label: chartLabels.pendingTransfers,
            value: residency.pendingTransfers,
            permission: Permission.ROOM_TRANSFER_READ,
          },
        ]
      : []),
    ...(registration
      ? [
          {
            label: chartLabels.pendingSummer,
            value: registration.pendingSummerChoices,
            permission: Permission.SUMMER_REGISTRATION_READ,
          },
        ]
      : []),
    {
      label: chartLabels.missingLeaders,
      value: operations?.roomsMissingLeader ?? 0,
      permission: Permission.ROOM_READ,
    },
  ]

  /** Việc có phát sinh đứng riêng; việc bằng 0 gom thành nhãn gọn để không chiếm chiều cao. */
  const activeQueue = queue.filter((item) => item.value > 0)
  const idleQueue = queue.filter((item) => item.value === 0)

  const facilityMetrics =
    infrastructure && capacity
      ? [
          { label: chartLabels.buildings, value: infrastructure.buildings },
          { label: chartLabels.totalRooms, value: infrastructure.rooms },
          { label: chartLabels.operational, value: capacity.operationalBeds },
          {
            label: chartLabels.maintenanceBeds,
            value: capacity.maintenanceBeds,
          },
          ...(residency
            ? [
                {
                  label: chartLabels.storageRecords,
                  value: residency.storageRecords,
                },
                {
                  label: chartLabels.storedPackages,
                  value: residency.storedPackages,
                },
              ]
            : []),
        ]
      : []

  return (
    <div className="dms-dashboard" aria-busy={isFetching}>
      <PageHeader
        title={
          <>
            <span className="dms-page-eyebrow">{chartLabels.eyebrow}</span>
            {chartLabels.title}
          </>
        }
        status={
          <span className="dms-dashboard-scope">
            {dashboard?.unitName ?? chartLabels.allUnits}
          </span>
        }
        subtitle={
          <>
            {chartLabels.hello} {me?.fullName}
            {dashboard && dataUpdatedAt > 0 && (
              <span className="dms-dashboard-updated">
                {' · '}
                {chartLabels.refreshedAt}{' '}
                {formatDateTime(new Date(dataUpdatedAt))}
              </span>
            )}
          </>
        }
        extra={
          <Space wrap>
            <Button
              icon={<ReloadOutlined />}
              loading={isFetching}
              onClick={() => {
                void refetch()
              }}
            >
              {chartLabels.refresh}
            </Button>
            {hasPermission(me, Permission.INVOICE_CREATE) && (
              <Button
                type="primary"
                icon={<FileAddOutlined />}
                onClick={() => setIssueOpen(true)}
              >
                {chartLabels.issue}
              </Button>
            )}
          </Space>
        }
      />
      {isLoading ? (
        <PageSkeleton rows={8} />
      ) : !dashboard ? (
        <Card>
          <Alert
            type="error"
            showIcon
            title={chartLabels.loadError}
            action={
              <Button
                onClick={() => {
                  void refetch()
                }}
              >
                {chartLabels.retry}
              </Button>
            }
          />
        </Card>
      ) : (
        <div className="dms-dashboard-body">
          {error && (
            <Alert type="warning" showIcon title={chartLabels.refreshError} />
          )}

          <section aria-labelledby="dms-dashboard-overview">
            <SectionHeading
              id="dms-dashboard-overview"
              title={chartLabels.overviewSection}
              note={chartLabels.scopeNote}
            />
            <div className="dms-dashboard-kpi-grid">
              {kpis.map((item) => (
                <KpiCard key={item.label} {...item} />
              ))}
            </div>
          </section>

          <section aria-labelledby="dms-dashboard-analysis">
            <SectionHeading
              id="dms-dashboard-analysis"
              title={chartLabels.analysisSection}
            />
            <Row gutter={[16, 16]} align="stretch">
              <Col xs={24} lg={12}>
                <BedCapacityChart capacity={dashboard.capacity} />
              </Col>
              <Col xs={24} lg={12}>
                <FinanceChart finance={dashboard.finance} />
              </Col>
            </Row>
          </section>

          <section aria-labelledby="dms-dashboard-follow-up">
            <SectionHeading
              id="dms-dashboard-follow-up"
              title={chartLabels.followUpSection}
            />
            <Row gutter={[16, 16]} align="stretch">
              <Col xs={24} lg={12}>
                <Card
                  title={chartLabels.queueTitle}
                  className="dms-dashboard-chart-card"
                >
                  {activeQueue.length > 0 ? (
                    <div className="dms-dashboard-queue">
                      <span className="dms-dashboard-queue-group">
                        {chartLabels.queueActive}
                      </span>
                      {activeQueue.map((item) => {
                        const target = targetFor(item.permission)
                        const content = (
                          <>
                            <span className="dms-dashboard-queue-label">
                              {item.label}
                            </span>
                            <span className="dms-dashboard-queue-value">
                              {formatNumber(item.value)}
                              {target && (
                                <ArrowRightOutlined
                                  className="dms-dashboard-queue-arrow"
                                  aria-hidden="true"
                                />
                              )}
                            </span>
                          </>
                        )
                        return target ? (
                          <Link
                            className="dms-dashboard-queue-row"
                            to={target}
                            key={item.label}
                          >
                            {content}
                          </Link>
                        ) : (
                          <div
                            className="dms-dashboard-queue-row"
                            key={item.label}
                          >
                            {content}
                          </div>
                        )
                      })}
                    </div>
                  ) : (
                    <span className="dms-dashboard-chart-note">
                      {chartLabels.queueClear}
                    </span>
                  )}
                  {idleQueue.length > 0 && (
                    <div className="dms-dashboard-queue-idle">
                      <span className="dms-dashboard-queue-group">
                        {chartLabels.queueIdle}
                      </span>
                      <div className="dms-dashboard-queue-chips">
                        {idleQueue.map((item) => {
                          const target = targetFor(item.permission)
                          const className = 'dms-dashboard-queue-chip'
                          return target ? (
                            <Link
                              className={className}
                              to={target}
                              key={item.label}
                            >
                              {item.label}
                            </Link>
                          ) : (
                            <span className={className} key={item.label}>
                              {item.label}
                            </span>
                          )
                        })}
                      </div>
                    </div>
                  )}
                </Card>
              </Col>
              <Col xs={24} lg={12}>
                <StatusBreakdown
                  title={chartLabels.ticketsTitle}
                  entity="ticket"
                  counts={dashboard.operations.ticketsByStatus}
                />
              </Col>
            </Row>
          </section>

          <Row gutter={[16, 16]} align="stretch">
            {infrastructure && (
              <Col xs={24} lg={12}>
                <Card
                  title={chartLabels.infrastructureTitle}
                  className="dms-dashboard-chart-card"
                >
                  <div className="dms-facility-grid">
                    {facilityMetrics.map((item) => (
                      <div className="dms-facility-item" key={item.label}>
                        <span className="dms-facility-value">
                          {formatNumber(item.value)}
                        </span>
                        <span className="dms-facility-label">
                          {item.label}
                        </span>
                      </div>
                    ))}
                  </div>
                  <div className="dms-dashboard-room-statuses">
                    <span className="dms-dashboard-chart-note">
                      {chartLabels.roomStatuses}
                    </span>
                    {Object.keys(infrastructure.roomsByStatus).length > 0 ? (
                      Object.entries(infrastructure.roomsByStatus).map(
                        ([status, count]) => (
                          <span
                            className="dms-dashboard-room-status"
                            key={status}
                          >
                            {roomOperation[status]?.label ?? status}:{' '}
                            <strong>{formatNumber(count)}</strong>
                          </span>
                        ),
                      )
                    ) : (
                      <span className="dms-dashboard-chart-note">
                        {chartLabels.noRoomStatuses}
                      </span>
                    )}
                  </div>
                </Card>
              </Col>
            )}
            <Col xs={24} lg={infrastructure ? 12 : 24}>
              <Card
                title={chartLabels.shortcutsTitle}
                className="dms-dashboard-chart-card"
              >
                <span className="dms-dashboard-chart-note">
                  {chartLabels.shortcutsHint}
                </span>
                {shortcuts.length > 0 ? (
                  <>
                    <div className="dms-dashboard-shortcuts">
                      {visibleShortcuts.map((item) => {
                        const Icon = item.icon
                        return (
                          <Link
                            className="dms-dashboard-shortcut"
                            to={item.path}
                            key={item.path}
                          >
                            <span
                              className="dms-dashboard-shortcut-icon"
                              aria-hidden="true"
                            >
                              {Icon && <Icon />}
                            </span>
                            <span className="dms-dashboard-shortcut-label">
                              {item.label}
                            </span>
                          </Link>
                        )
                      })}
                      {Array.from(
                        { length: shortcutPlaceholders },
                        (_, index) => (
                          <span
                            className="dms-dashboard-shortcut dms-dashboard-shortcut-placeholder"
                            key={`shortcut-placeholder-${index}`}
                            aria-hidden="true"
                          />
                        ),
                      )}
                    </div>
                    <div
                      className="dms-dashboard-shortcuts-pager"
                      role="group"
                      aria-label={chartLabels.shortcutsPager}
                    >
                      <Button
                        type="text"
                        size="small"
                        shape="circle"
                        className="dms-dashboard-shortcuts-nav"
                        icon={<LeftOutlined />}
                        aria-label={chartLabels.shortcutsPrevious}
                        disabled={currentShortcutPage <= 0}
                        onClick={() => changeShortcutPage(-1)}
                      />
                      <span
                        className="dms-dashboard-shortcuts-page"
                        aria-live="polite"
                      >
                        {currentShortcutPage + 1}/{shortcutPageCount}
                      </span>
                      <Button
                        type="text"
                        size="small"
                        shape="circle"
                        className="dms-dashboard-shortcuts-nav"
                        icon={<RightOutlined />}
                        aria-label={chartLabels.shortcutsNext}
                        disabled={
                          currentShortcutPage >= shortcutPageCount - 1
                        }
                        onClick={() => changeShortcutPage(1)}
                      />
                    </div>
                  </>
                ) : (
                  <span className="dms-dashboard-chart-note">
                    {chartLabels.noShortcuts}
                  </span>
                )}
              </Card>
            </Col>
          </Row>
        </div>
      )}
      <IssueInvoicesModal
        open={issueOpen}
        onClose={() => setIssueOpen(false)}
        currentUnitId={me?.managementUnit?.id}
      />
    </div>
  )
}
