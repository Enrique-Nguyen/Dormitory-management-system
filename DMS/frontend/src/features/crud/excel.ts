import type ExcelJS from 'exceljs'
import { createSearchRequest, isFieldVisible, readValue, searchByPage, type CrudQuery } from './api'
import { excelUiLabels as labels } from './resources'
import type { CrudField, CrudRecord, CrudResource } from './types'

export const EXCEL_MAX_BYTES = 5 * 1024 * 1024
export const EXCEL_MAX_IMPORT_ROWS = 5000
export const EXCEL_MAX_EXPORT_ROWS = 10000
export interface ExcelColumn { key: string; label: string }
export interface ImportWorkbookRow {
  rowNumber: number
  payload: Record<string, unknown>
  errors: string[]
}

const secretKey = /password|secret|token|credential|authorization/i
const positiveId = (value: unknown): value is number =>
  typeof value === 'number' && Number.isSafeInteger(value) && value > 0

function safeValue(value: unknown): ExcelJS.CellValue {
  if (value == null) return null
  if (typeof value === 'boolean') return value
  if (typeof value === 'number') return Number.isFinite(value) ? value : String(value)
  if (value instanceof Date) return value.toISOString()
  // A string cell is never a formula, even when it begins with =, +, - or @.
  if (typeof value === 'object') return JSON.stringify(value, (key, nested: unknown) => secretKey.test(key) ? undefined : nested)
  return String(value)
}

async function saveWorkbook(workbook: ExcelJS.Workbook, title: string): Promise<void> {
  const buffer = await workbook.xlsx.writeBuffer()
  const blob = new Blob([new Uint8Array(buffer)], {
    type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
  })
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  const fileName = Array.from(title, (character) =>
    character.charCodeAt(0) < 32 || /[<>:"/\\|?*]/.test(character) ? '_' : character,
  ).join('')
  link.download = `${fileName}.xlsx`
  document.body.appendChild(link)
  link.click()
  link.remove()
  window.setTimeout(() => URL.revokeObjectURL(url), 30000)
}

function addDataSheet(workbook: ExcelJS.Workbook, columns: ExcelColumn[], rows: Record<string, unknown>[]) {
  const sheet = workbook.addWorksheet(labels.dataSheet.slice(0, 31))
  sheet.columns = columns.map((column) => ({ header: column.label, key: column.key, width: 26 }))
  rows.forEach((row) => sheet.addRow(columns.map((column) => safeValue(row[column.key] ?? readValue(row, column.key)))))
  sheet.getRow(1).font = { bold: true }
  sheet.views = [{ state: 'frozen', ySplit: 1 }]
  return sheet
}

/** Xuất dữ liệu thông thường; không bao giờ đưa trường bí mật vào workbook. */
export async function downloadWorkbook(title: string, columns: ExcelColumn[], rows: Record<string, unknown>[]): Promise<void> {
  const { default: ExcelJS } = await import('exceljs')
  const workbook = new ExcelJS.Workbook()
  addDataSheet(workbook, columns.filter((column) => !secretKey.test(column.key)), rows)
  await saveWorkbook(workbook, title)
}

/** Chỉ gọi cho kết quả tạo tài khoản một lần, không dùng dữ liệu danh sách/cache. */
export async function downloadCredentialWorkbook(title: string, rows: Record<string, unknown>[]): Promise<void> {
  const { default: ExcelJS } = await import('exceljs')
  const workbook = new ExcelJS.Workbook()
  addDataSheet(workbook, [
    { key: 'id', label: labels.id },
    { key: 'username', label: 'username' },
    { key: 'temporaryPassword', label: 'temporaryPassword' },
  ], rows.map(({ id, username, temporaryPassword }) => ({ id, username, temporaryPassword })))
  await saveWorkbook(workbook, title)
}

/** Giữ nguyên bộ lọc máy chủ; báo lỗi thay vì xuất một tập dữ liệu bị cắt. */
export async function exportFilteredRecords(config: CrudResource, query: CrudQuery): Promise<CrudRecord[]> {
  const records: CrudRecord[] = []
  for (let page = 1; ; page++) {
    const result = await searchByPage(config.endpoint, createSearchRequest({ ...query, page, size: 100 }, config))
    if (result.totalElements > EXCEL_MAX_EXPORT_ROWS || records.length + result.content.length > EXCEL_MAX_EXPORT_ROWS)
      throw new Error(labels.tooManyExportRows)
    records.push(...result.content)
    if (!result.content.length || records.length >= result.totalElements) break
    if (records.length >= EXCEL_MAX_EXPORT_ROWS) throw new Error(labels.tooManyExportRows)
  }
  return records
}

export function workbookFields(config: CrudResource): CrudField[] {
  return [...new Map([...config.fields, ...(config.createFields ?? []), ...(config.editFields ?? [])]
    .filter((field) => !field.formOnly && !field.sensitive && !secretKey.test(field.name))
    .map((field) => [field.name, field])).values()]
}

export async function downloadImportTemplate(config: CrudResource): Promise<void> {
  const { default: ExcelJS } = await import('exceljs')
  const workbook = new ExcelJS.Workbook()
  const idKey = config.idKey ?? 'id'
  const fields = workbookFields(config).filter((field) => field.name !== idKey)
  const sheet = addDataSheet(workbook, [
    { key: idKey, label: `${labels.id} [${idKey}]` },
    ...fields.map((field) => ({ key: field.name, label: `${field.label} [${field.name}]` })),
  ], [])
  sheet.name = labels.templateSheet.slice(0, 31)
  // Empty text-formatted cells preserve identifiers with leading zeroes in Excel.
  fields.forEach((field, index) => {
    if ((!field.lookup || (field.lookupValue && !['id', 'floor'].includes(field.lookupValue))) && !['number', 'boolean', 'date', 'datetime'].includes(field.kind ?? 'text')) sheet.getColumn(index + 2).numFmt = '@'
  })
  const notes = workbook.addWorksheet(labels.fieldsSheet.slice(0, 31))
  notes.addRow([labels.instructions, labels.format])
  notes.addRow([labels.field, labels.type, labels.requiredColumn, labels.options, labels.reference])
  fields.forEach((field) => notes.addRow([
    `${field.label} [${field.name}]`, field.kind ?? 'text', field.required ? labels.yes : labels.no,
    field.options?.map((option) => `${option.value}: ${option.label}`).join('; ') ?? '',
    [field.lookup, field.visibleWhen ? `${field.visibleWhen.field}=${field.visibleWhen.value}` : '', field.hint].filter(Boolean).join('; '),
  ]))
  notes.columns.forEach((column) => { column.width = 35 })
  await saveWorkbook(workbook, `${config.title}-${labels.fileNameSuffix}`)
}

function cellValue(cell: ExcelJS.Cell): unknown {
  if (cell.formula !== undefined) throw new Error(labels.formula)
  const value = cell.value
  if (value && typeof value === 'object' && !(value instanceof Date)) throw new Error(labels.invalidValue)
  return value
}

function dateValue(value: unknown, kind: 'date' | 'datetime', date1904: boolean): string {
  let date: Date
  if (value instanceof Date) date = value
  else if (typeof value === 'number') {
    if (!Number.isFinite(value) || value < 0 || (!date1904 && value >= 60 && value < 61)) throw new Error(labels.invalidValue)
    const offset = date1904 ? 24107 : value < 60 ? 25568 : 25569
    date = new Date(Math.round((value - offset) * 86400000))
  } else {
    const text = String(value)
    if (!/^\d{4}-\d{2}-\d{2}(?:T\d{2}:\d{2}(?::\d{2}(?:\.\d+)?)?(?:Z|[+-]\d{2}:\d{2}))?$/.test(text)) throw new Error(labels.invalidValue)
    const day = text.slice(0, 10)
    const parsedDay = new Date(`${day}T00:00:00Z`)
    if (!Number.isFinite(parsedDay.getTime()) || parsedDay.toISOString().slice(0, 10) !== day) throw new Error(labels.invalidValue)
    date = new Date(text)
    if (kind === 'date') return day
  }
  if (!Number.isFinite(date.getTime())) throw new Error(labels.invalidValue)
  return kind === 'date' ? date.toISOString().slice(0, 10) : date.toISOString()
}

function convertValue(value: unknown, field: CrudField, cell: ExcelJS.Cell, date1904: boolean): unknown {
  if (value == null || value === '') return undefined
  if (field.multiple) {
    const values: unknown = typeof value === 'string' ? JSON.parse(value) : value
    if (!Array.isArray(values) || values.some((item) => item == null)) throw new Error(labels.invalidValue)
    return values.map((item) => convertValue(item, { ...field, multiple: false }, cell, date1904))
  }
  if (field.lookup) {
    if (field.lookupValue && !['id', 'floor'].includes(field.lookupValue)) {
      if (typeof value !== 'string' || !value.trim() || (field.maxLength != null && value.length > field.maxLength)) throw new Error(labels.invalidValue)
      return value.trim()
    }
    const id = typeof value === 'string' && /^\d+$/.test(value) ? Number(value) : value
    if (field.lookupValue === 'floor') {
      if (typeof id !== 'number' || !Number.isSafeInteger(id) || id < (field.min ?? 0) || (field.max != null && id > field.max)) throw new Error(labels.invalidValue)
    } else if (!positiveId(id)) throw new Error(labels.invalidValue)
    return id
  }
  if (field.options) {
    const matches = field.options.filter((option) => String(option.value) === String(value) || option.label === value)
    if (matches.length !== 1) throw new Error(labels.invalidValue)
    return matches[0].value
  }
  if (field.kind === 'boolean') {
    if (typeof value === 'boolean') return value
    if (value === 'true') return true
    if (value === 'false') return false
    throw new Error(labels.invalidValue)
  }
  if (field.kind === 'date' || field.kind === 'datetime') return dateValue(value, field.kind, date1904)
  if (field.kind === 'number') {
    if (typeof value !== 'number' && (typeof value !== 'string' || !/^[+-]?(?:\d+(?:\.\d*)?|\.\d+)$/.test(value))) throw new Error(labels.invalidValue)
    const number = Number(value)
    if (!Number.isFinite(number) || Math.abs(number) > Number.MAX_SAFE_INTEGER || (field.min != null && number < field.min) || (field.max != null && number > field.max)) throw new Error(labels.invalidValue)
    return number
  }
  if (value instanceof Date || typeof value === 'boolean' || (typeof value === 'number' && !Number.isSafeInteger(value))) throw new Error(labels.invalidValue)
  const text = typeof value === 'number' && /^0+$/.test(cell.numFmt)
    ? String(value).padStart(cell.numFmt.length, '0') : String(value)
  if (field.maxLength != null && text.length > field.maxLength) throw new Error(labels.invalidValue)
  return text
}

/** Phân tích cục bộ; không ghi dữ liệu và không đánh giá công thức trong workbook. */
export async function parseImportWorkbook(file: File, fields: CrudField[], idKey = 'id'): Promise<ImportWorkbookRow[]> {
  if (!file.name.toLowerCase().endsWith('.xlsx') || !file.size || file.size > EXCEL_MAX_BYTES) throw new Error(labels.invalidFile)
  const { default: ExcelJS } = await import('exceljs')
  const workbook = new ExcelJS.Workbook()
  try { await workbook.xlsx.load(await file.arrayBuffer()) } catch { throw new Error(labels.invalidFile) }
  const sheet = workbook.worksheets[0]
  if (!sheet || sheet.columnCount > fields.length + 1) throw new Error(labels.invalidHeaders)
  const allowed = fields.filter((field) => !field.sensitive && !secretKey.test(field.name) && field.name !== idKey)
  const numericId = idKey === 'id' || idKey.endsWith('Id')
  const idField: CrudField = { name: idKey, label: labels.id, kind: numericId ? 'number' : 'text', min: numericId ? 1 : undefined }
  const candidates = [idField, ...allowed]
  const columns = new Map<number, CrudField>()
  const seen = new Set<string>()
  sheet.getRow(1).eachCell((cell, index) => {
    const header = String(cellValue(cell) ?? '').trim()
    const matches = candidates.filter((field) => header === field.name || header === field.label || header === `${field.label} [${field.name}]`)
    if (matches.length !== 1) throw new Error(`${labels.unknownColumn}: ${header}`)
    const field = matches[0]
    if (seen.has(field.name)) throw new Error(`${labels.duplicateColumn}: ${header}`)
    seen.add(field.name)
    columns.set(index, field)
  })
  if (!columns.size) throw new Error(labels.invalidHeaders)
  const rows: ImportWorkbookRow[] = []
  sheet.eachRow((row, rowNumber) => {
    if (rowNumber === 1 || !row.values || !row.hasValues) return
    let nonBlank = false
    row.eachCell((cell) => { if (cell.value != null && cell.value !== '') nonBlank = true })
    if (!nonBlank) return
    if (rows.length >= EXCEL_MAX_IMPORT_ROWS) throw new Error(labels.tooManyRows)
    const result: ImportWorkbookRow = { rowNumber, payload: {}, errors: [] }
    row.eachCell((cell, index) => {
      if (!columns.has(index) && cell.value != null && cell.value !== '') result.errors.push(labels.unknownColumn)
    })
    columns.forEach((field, index) => {
      try {
        const value = convertValue(cellValue(row.getCell(index)), field, row.getCell(index), Boolean(workbook.properties.date1904))
        if (value !== undefined) result.payload[field.name] = value
        if (numericId && field.name === idKey && value !== undefined && !positiveId(value)) result.errors.push(`${field.label}: ${labels.invalidValue}`)
      } catch (error) { result.errors.push(`${field.label}: ${error instanceof Error && error.message === labels.formula ? labels.formula : labels.invalidValue}`) }
    })
    const update = result.payload[idKey] !== undefined
    allowed.forEach((field) => {
      const visible = !field.visibleWhen || isFieldVisible(field, result.payload[field.visibleWhen.field])
      const value = result.payload[field.name]
      const applicable = update ? !field.createOnly : !field.editOnly
      if (applicable && visible && field.required && !update && (value == null || value === '' || (typeof value === 'string' && !value.trim()) || (Array.isArray(value) && !value.length)))
        result.errors.push(`${field.label}: ${labels.required}`)
      if (!visible) delete result.payload[field.name]
    })
    rows.push(result)
  })
  if (!rows.length) throw new Error(labels.noRows)
  return rows
}
