import type { ComponentType } from 'react'
import type { Role } from '../../constants/authorization'
import ExtensionDetailPage from '../../pages/staff/ExtensionDetailPage'
import RoomDetailPage from '../../pages/staff/RoomDetailPage'
import TransferReviewPage from '../../pages/staff/TransferReviewPage'
import ContractDetailView from '../contract/ContractDetailView'
import MaintenanceDetailView from '../maintenance/MaintenanceDetailView'
import ApplicationDetailView from '../registration/ApplicationDetailView'
import SummerChoiceDetailView from '../registration/SummerChoiceDetailView'

export interface RecordDetailContentProps {
  resource: string
  recordId: number
  role?: Role
  readOnly?: boolean
}

type EmbeddedDetailProps = Omit<RecordDetailContentProps, 'resource'> & {
  embedded: true
}

const detailViews = {
  applications: ({ recordId, ...options }: EmbeddedDetailProps) => (
    <ApplicationDetailView applicationId={recordId} {...options} />
  ),
  contracts: ({ recordId, ...options }: EmbeddedDetailProps) => (
    <ContractDetailView contractId={recordId} {...options} />
  ),
  summer: ({ recordId, ...options }: EmbeddedDetailProps) => (
    <SummerChoiceDetailView summerChoiceId={recordId} {...options} />
  ),
  maintenance: ({ recordId, ...options }: EmbeddedDetailProps) => (
    <MaintenanceDetailView ticketId={recordId} {...options} />
  ),
  extensions: ({ recordId, ...options }: EmbeddedDetailProps) => (
    <ExtensionDetailPage extensionId={recordId} {...options} />
  ),
  transfers: ({ recordId, ...options }: EmbeddedDetailProps) => (
    <TransferReviewPage transferId={recordId} {...options} />
  ),
  rooms: ({ recordId, ...options }: EmbeddedDetailProps) => (
    <RoomDetailPage roomId={recordId} {...options} />
  ),
}

const contentByResource: Record<string, ComponentType<EmbeddedDetailProps>> = {
  ...detailViews,
  myApplications: detailViews.applications,
  myContracts: detailViews.contracts,
  myRoom: detailViews.rooms,
}

// oxlint-disable-next-line react/only-export-components -- Capability detection is part of this adapter's public API.
export function hasRecordDetailContent(resource: string): boolean {
  return Object.hasOwn(contentByResource, resource)
}

/** Reuse detail queries and workflows without replacing the surrounding list route. */
export default function RecordDetailContent({
  resource,
  recordId,
  role,
  readOnly = true,
}: RecordDetailContentProps) {
  if (
    !hasRecordDetailContent(resource) ||
    !Number.isSafeInteger(recordId) ||
    recordId <= 0
  ) return null

  const DetailContent = contentByResource[resource]
  return (
    <DetailContent
      key={`${resource}-${recordId}`}
      recordId={recordId}
      role={role}
      embedded
      readOnly={readOnly}
    />
  )
}
