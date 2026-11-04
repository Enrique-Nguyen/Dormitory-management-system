import { App, Flex, Space, Typography, Upload } from 'antd'
import axios from 'axios'
import { DownloadOutlined, UploadOutlined } from '@ant-design/icons'
import { useEffect, useRef, useState } from 'react'
import DataTable from '../../components/ui/DataTable'
import ConfirmDialog from '../../components/ui/ConfirmDialog'
import { Alert, Button, Card, Modal, Tag } from '../../components/ui/Surface'
import { errorMessage } from '../../utils/errorMessages'
import { CRUD_DEFAULT_PAGE_SIZE, CRUD_PAGE_SIZES, validationFieldErrors } from './api'
import {
  downloadCredentialWorkbook, downloadImportTemplate, EXCEL_MAX_BYTES,
  parseImportWorkbook, workbookFields, type ImportWorkbookRow,
} from './excel'
import { excelUiLabels as labels } from './resources'
import type { CrudResource } from './types'

interface Props {
  resource: string
  config: CrudResource
  canCreate: boolean
  canUpdate: boolean
  onSave: (payload: Record<string, unknown>) => Promise<unknown>
  onComplete: () => Promise<void> | void
  triggerLabel?: string
  onBlockedChange?: (blocked: boolean) => void
}
type RowState = 'ready' | 'success' | 'failed'
interface PreviewRow extends ImportWorkbookRow { state: RowState; fingerprint: string }

/** Mỗi dòng là một lần ghi riêng; mật khẩu chỉ giữ trong state cục bộ. */
export default function CrudExcelPanel({ resource, config, canCreate, canUpdate, onSave, onComplete, triggerLabel = labels.import, onBlockedChange }: Props) {
  const { message } = App.useApp()
  const [open, setOpen] = useState(false)
  const [file, setFile] = useState<File>()
  const [rows, setRows] = useState<PreviewRow[]>([])
  const [credentials, setCredentials] = useState<Record<string, unknown>[]>([])
  const [downloaded, setDownloaded] = useState(false)
  const [busy, setBusy] = useState(false)
  const [confirming, setConfirming] = useState(false)
  const [attempted, setAttempted] = useState(false)
  const pending = useRef(false)
  const saved = useRef(new Set<string>())
  const blocked = busy || (credentials.length > 0 && !downloaded)
  const idKey = config.idKey ?? 'id'
  const readyRows = rows.filter((row) => row.state === 'ready' && !row.errors.length)

  useEffect(() => { onBlockedChange?.(blocked) }, [blocked, onBlockedChange])
  useEffect(() => () => onBlockedChange?.(false), [onBlockedChange])
  useEffect(() => {
    if (!blocked) return
    const guard = (event: BeforeUnloadEvent) => { event.preventDefault(); event.returnValue = '' }
    window.addEventListener('beforeunload', guard)
    return () => window.removeEventListener('beforeunload', guard)
  }, [blocked])

  async function preview() {
    if (!file || pending.current || blocked || attempted) return
    pending.current = true
    setBusy(true)
    try {
      const parsed = await parseImportWorkbook(file, workbookFields(config), idKey)
      setRows(parsed.map((row) => {
        const fingerprint = `${resource}:${JSON.stringify(Object.fromEntries(Object.entries(row.payload).sort(([left], [right]) => left.localeCompare(right))))}`
        const permission = row.payload[idKey] !== undefined ? canUpdate : canCreate
        return {
          ...row, fingerprint,
          errors: permission ? row.errors : [...row.errors, labels.invalidValue],
          state: saved.current.has(fingerprint) ? 'success' : 'ready',
        }
      }))
    } catch (error) {
      setRows([])
      message.error(error instanceof Error ? error.message : labels.invalidFile)
    } finally { pending.current = false; setBusy(false) }
  }

  async function apply() {
    if (pending.current || attempted || !readyRows.length || blocked) return
    pending.current = true
    setBusy(true)
    setAttempted(true)
    const next = rows.map((row) => ({ ...row, errors: [...row.errors] }))
    try {
      // Never retry writes: even an ambiguous network failure may have committed.
      for (const row of next) {
        if (saved.current.has(row.fingerprint)) { row.state = 'success'; continue }
        if (row.state !== 'ready' || row.errors.length) continue
        if (row.payload[idKey] !== undefined ? !canUpdate : !canCreate) {
          row.state = 'failed'
          row.errors.push(labels.invalidValue)
          continue
        }
        try {
          const response = await onSave({ ...row.payload })
          row.state = 'success'
          saved.current.add(row.fingerprint)
          if (response && typeof response === 'object') {
            const record = response as Record<string, unknown>
            if (typeof record.temporaryPassword === 'string' && record.temporaryPassword) {
              setCredentials((previous) => [...previous, {
                id: record[idKey] ?? record.id, username: record.username ?? row.payload.username,
                temporaryPassword: record.temporaryPassword,
              }])
              setDownloaded(false)
            }
          }
        } catch (error) {
          row.state = 'failed'
          row.errors.push(!axios.isAxiosError(error) && error instanceof Error ? error.message : errorMessage(error), ...validationFieldErrors(error).map((item) => `${item.field}: ${item.message}`))
        }
        setRows(next.map((item) => ({ ...item, errors: [...item.errors] })))
      }
      setRows(next)
      try { await onComplete() } catch (error) { message.error(errorMessage(error)) }
      if (next.some((row) => row.state === 'failed' || row.errors.length)) message.warning(labels.partialWarning)
      else message.success(labels.complete)
    } finally {
      pending.current = false
      setBusy(false)
      setConfirming(false)
    }
  }

  async function downloadCredentials() {
    if (pending.current || !credentials.length) return
    pending.current = true
    try {
      await downloadCredentialWorkbook(`${config.title}-${labels.credentials}`, credentials)
      setDownloaded(true)
    } catch (error) { message.error(errorMessage(error)) }
    finally { pending.current = false }
  }

  return <>
    <Button icon={<UploadOutlined />} disabled={!canCreate && !canUpdate} onClick={() => setOpen(true)}>{triggerLabel}</Button>
    <Modal open={open} title={`${config.title} — ${labels.preview}`} width={1000} footer={null}
      closable={!blocked} keyboard={!blocked} mask={{ closable: false }}
      onCancel={() => { if (!pending.current && !blocked) setOpen(false) }}>
      <Space direction="vertical" size="large" className="dms-full-width">
        <Alert type="info" showIcon title={labels.instructions} description={labels.format} />
        <Alert type="warning" showIcon title={labels.partialWarning} />
        <Card>
          <Flex gap={12} wrap align="center">
            <Upload accept=".xlsx" maxCount={1} disabled={blocked}
              fileList={file ? [{ uid: 'workbook', name: file.name, status: 'done' }] : []}
              beforeUpload={(selected) => {
                if (pending.current || blocked) return Upload.LIST_IGNORE
                if (!selected.name.toLowerCase().endsWith('.xlsx') || !selected.size || selected.size > EXCEL_MAX_BYTES) {
                  message.error(labels.invalidFile)
                  return Upload.LIST_IGNORE
                }
                setFile(selected); setRows([]); setAttempted(false)
                setCredentials([]); setDownloaded(false)
                return false
              }}
              onRemove={() => {
                if (pending.current || blocked) return false
                setFile(undefined); setRows([]); setAttempted(false)
                setCredentials([]); setDownloaded(false)
              }}>
              <Button icon={<UploadOutlined />} disabled={blocked}>{labels.choose}</Button>
            </Upload>
            <Button icon={<DownloadOutlined />} disabled={busy} onClick={() => {
              void downloadImportTemplate(config).catch((error: unknown) => message.error(errorMessage(error)))
            }}>{labels.template}</Button>
            <Button loading={busy && !attempted} disabled={!file || blocked || attempted}
              onClick={() => void preview()}>{labels.preview}</Button>
            <Button type="primary" disabled={blocked || attempted || !readyRows.length}
              onClick={() => setConfirming(true)}>{labels.apply}</Button>
          </Flex>
        </Card>
        {!!rows.length && <>
          <Flex gap={16} wrap>
            {([
              [labels.total, rows.length], [labels.valid, rows.filter((row) => !row.errors.length).length],
              [labels.errors, rows.filter((row) => row.errors.length > 0).length],
              [labels.saved, rows.filter((row) => row.state === 'success').length],
            ] as const).map(([label, count]) => <Typography.Text key={label}>{label}: <strong>{count}</strong></Typography.Text>)}
          </Flex>
          <DataTable<PreviewRow> rowKey="rowNumber" showStt={false} dataSource={rows}
            pagination={{ pageSize: CRUD_DEFAULT_PAGE_SIZE, showSizeChanger: true, pageSizeOptions: CRUD_PAGE_SIZES }}
            columns={[
              { title: labels.row, dataIndex: 'rowNumber', width: 80 },
              { title: labels.preview, render: (_, row) => JSON.stringify(row.payload) },
              { title: labels.result, render: (_, row) => <Tag>{row.state === 'success' ? labels.success : row.state === 'failed' || row.errors.length ? labels.failed : labels.ready}</Tag> },
              { title: labels.reason, render: (_, row) => row.errors.join('; ') },
            ]} />
        </>}
        {credentials.length > 0 && <Alert type="warning" showIcon title={labels.credentialWarning}
          action={<Button icon={<DownloadOutlined />} disabled={busy} onClick={() => void downloadCredentials()}>{labels.credentials}</Button>} />}
      </Space>
    </Modal>
    {confirming && <ConfirmDialog open title={labels.confirm} description={labels.confirmDescription}
      okText={labels.apply} onConfirm={apply} onClose={() => { if (!pending.current) setConfirming(false) }} />}
  </>
}
