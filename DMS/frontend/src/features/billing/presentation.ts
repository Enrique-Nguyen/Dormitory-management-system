import dayjs from 'dayjs'
import { InvoiceType } from '../../constants/statusConfig'
import { formatMonth } from '../../utils/format'
import { billingUiLabels } from '../crud/resources'
import type { CrudRecord } from '../crud/types'

/** Kỳ thu lấy từ học kỳ liên kết hoặc tháng điện nước, không suy từ ngày phát hành. */
export function formatInvoicePeriod(invoice: CrudRecord, fullSemesterName = false): string {
  if (invoice.type === InvoiceType.ROOM_FEE) {
    const name = typeof invoice.semesterName === 'string' ? invoice.semesterName.trim() : ''
    const typeLabel = billingUiLabels.invoiceSemesterTypes[String(invoice.semesterType)]
    const semester = fullSemesterName ? name || typeLabel : typeLabel || name
    const year = typeof invoice.academicYear === 'string' ? invoice.academicYear.trim() : ''
    return semester && year
      ? `${semester} · ${year}`
      : invoice.semesterId != null
        ? billingUiLabels.invoiceSemesterReference.replace('{id}', String(invoice.semesterId))
        : billingUiLabels.invoiceMissingPeriod
  }
  if (invoice.type === InvoiceType.UTILITY
    && typeof invoice.billingMonth === 'string'
    && invoice.billingMonth.trim()
    && dayjs(invoice.billingMonth).isValid()) {
    return `${billingUiLabels.invoiceMonthPrefix} ${formatMonth(invoice.billingMonth)}`
  }
  return billingUiLabels.invoiceMissingPeriod
}
