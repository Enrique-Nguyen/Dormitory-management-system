import { App, Flex, Space, Typography, Upload } from 'antd'
import { DownloadOutlined, UploadOutlined } from '@ant-design/icons'
import { useRef, useState } from 'react'
import { useQueryClient } from '@tanstack/react-query'
import DataTable from '../../components/ui/DataTable'
import { useConfirm } from '../../components/ui/useConfirm'
import { Alert, Button, Card, Tag } from '../../components/ui/Surface'
import { Permission } from '../../constants/authorization'
import { StudentImportResult } from '../../constants/statusConfig'
import { studentImportLabels as labels } from '../crud/resources'
import { useMe } from './useMe'
import { hasPermission } from '../../services/api/auth'
import { api } from '../../services/api/client'
import { errorMessage } from '../../utils/errorMessages'

interface ImportRow {
  rowNumber: number
  studentCode: string
  result: (typeof StudentImportResult)[keyof typeof StudentImportResult]
  errors: { column: string; code: string }[]
}

interface ImportResult {
  dryRun: boolean
  totalRows: number
  validRows: number
  createdRows: number
  existingRows: number
  errorRows: number
  rows: ImportRow[]
  createdAccounts: { id: number; username: string; temporaryPassword: string }[]
}

function download(blob: Blob, name: string) {
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = name
  document.body.appendChild(link)
  link.click()
  link.remove()
  window.setTimeout(() => URL.revokeObjectURL(url), 30000)
}

/** Kết quả có mật khẩu chỉ giữ trong state của màn, không đặt vào query cache hay storage. */
export default function StudentImportPanel() {
  const { data: me } = useMe()
  const { message } = App.useApp()
  const { confirm, dialog } = useConfirm()
  const queries = useQueryClient()
  const [file, setFile] = useState<File>()
  const [result, setResult] = useState<ImportResult>()
  const [busy, setBusy] = useState(false)
  // Ref chặn nhấp hai lần cả trước khi React render lại trạng thái disabled.
  const pending = useRef(false)
  const canImport = hasPermission(me, Permission.STUDENT_CREATE)

  async function submit(dryRun: boolean) {
    if (!file || pending.current || !canImport) return
    pending.current = true
    setBusy(true)
    const form = new FormData()
    form.append('file', file)
    try {
      const { data } = await api.post<ImportResult>('/admin/student-imports', form, { params: { dryRun } })
      setResult(data)
      if (!dryRun) {
        message.success(labels.complete)
        void queries.invalidateQueries({ queryKey: ['crud'] })
      }
    } catch (error) {
      // Không giữ preview đã cũ để người dùng vô tình gửi lại một thao tác ghi thất bại.
      setResult(undefined)
      message.error(errorMessage(error))
      if (!dryRun) throw error
    } finally {
      pending.current = false
      setBusy(false)
    }
  }

  async function template() {
    try {
      const response = await api.get('/admin/student-imports/template', { responseType: 'blob' })
      download(response.data, labels.templateFileName)
    } catch (error) {
      message.error(errorMessage(error))
    }
  }

  function downloadAccounts() {
    if (!result?.createdAccounts.length) return
    const csv = [labels.csvHeaders, ...result.createdAccounts.map((account) => [account.username, account.temporaryPassword])]
      .map((row) => row.map((value) => `"${value.replaceAll('"', '""')}"`).join(','))
      .join('\r\n')
    download(new Blob(['\uFEFF', csv], { type: 'text/csv;charset=utf-8' }), labels.fileName)
  }

  return (
    <Space direction="vertical" size="large" className="dms-full-width">
      <Alert type="info" showIcon message={labels.instructions} description={labels.format} />
      <Card>
        <Flex gap={12} wrap align="center">
          <Upload accept=".xlsx" maxCount={1} disabled={busy || !canImport}
            fileList={file ? [{ uid: 'selected', name: file.name, status: 'done' }] : []}
            beforeUpload={(selected) => {
              if (!selected.name.toLowerCase().endsWith('.xlsx') || !selected.size || selected.size > labels.maxBytes) {
                message.error(labels.invalidFile)
                return Upload.LIST_IGNORE
              }
              setFile(selected)
              setResult(undefined)
              return false
            }}
            onRemove={() => { setFile(undefined); setResult(undefined) }}>
            <Button icon={<UploadOutlined />} disabled={busy || !canImport}>{labels.choose}</Button>
          </Upload>
          <Button onClick={() => void template()} icon={<DownloadOutlined />} disabled={!canImport || busy}>{labels.template}</Button>
          <Button type="primary" loading={busy} disabled={!file || !canImport || busy || Boolean(result?.createdAccounts.length)}
            onClick={() => void submit(true)}>{labels.preview}</Button>
          {result?.dryRun && result.validRows > 0 && (
            <Button type="primary" disabled={busy || !canImport} onClick={() => confirm({
              title: labels.confirm, description: labels.confirmDescription, okText: labels.apply,
              onConfirm: () => submit(false),
            })}>{labels.apply}</Button>
          )}
        </Flex>
      </Card>
      {result && <>
        <Flex gap={16} wrap>
          {([
            [labels.total, result.totalRows], [labels.valid, result.validRows],
            [labels.existing, result.existingRows], [labels.errors, result.errorRows],
            [labels.created, result.createdRows],
          ] as const).map(([label, count]) => <Typography.Text key={label}>{label}: <strong>{count}</strong></Typography.Text>)}
        </Flex>
        {result.createdAccounts.length > 0 && <Alert type="success" showIcon message={labels.credentialWarning}
          action={<Button onClick={downloadAccounts} icon={<DownloadOutlined />}>{labels.credentials}</Button>} />}
        <DataTable<ImportRow> rowKey="rowNumber" showStt={false} dataSource={result.rows}
          pagination={{ pageSize: 20, showSizeChanger: true, pageSizeOptions: [20, 50, 100] }}
          columns={[
            { title: labels.row, dataIndex: 'rowNumber', width: 110 },
            { title: labels.studentCode, dataIndex: 'studentCode' },
            { title: labels.result, dataIndex: 'result', render: (value: ImportRow['result']) =>
              <Tag>{value === StudentImportResult.EXISTS ? labels.existing : labels.errors}</Tag> },
            { title: labels.reason, render: (_, row) => row.errors.map((error) =>
              `${labels.columns[error.column] ?? error.column}: ${labels.errorCodes[error.code] ?? error.code}`).join('; ') },
          ]} />
      </>}
      {dialog}
    </Space>
  )
}
