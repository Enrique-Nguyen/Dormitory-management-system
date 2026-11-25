import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { App } from 'antd'
import { useCallback, useEffect, useMemo, useState } from 'react'
import {
  Alert,
  BedPicker,
  Button,
  Checkbox,
  FormField,
  FormGrid,
  Input,
  InputNumber,
  Modal,
  Radio,
  RadioGroup,
  TextArea,
} from '../../components'
import { PaymentMethod } from '../../constants/statusConfig'
import { useApiErrorHandler } from '../../hooks/useApiErrorHandler'
import { assignApplicationContract } from '../../services/api/registration'
import { errorMessage } from '../../utils/errorMessages'
import { formatBedCode, formatMoney } from '../../utils/format'
import { getLookupRecords } from '../crud/api'
import type { CrudRecord } from '../crud/types'
import { useAvailableBeds } from '../room'
import { useAssignCatalog } from '../registration/useRegistration'
import ContractApplicationPreview from './ContractApplicationPreview'

interface Props {
  open: boolean
  onClose: () => void
  /** Gọi sau khi lưu thành công để danh sách hợp đồng tải lại. */
  onCreated: () => void
}

interface Errors {
  applicationId?: string
  bed?: string
  dates?: string
  priceMain?: string
  priceSummer?: string
  paper?: string
  deposit?: string
  note?: string
}

const STALE_MS = 5 * 60 * 1000

/**
 * Thêm hợp đồng ở một màn duy nhất: chọn đơn đã duyệt, chọn giường, chốt ngày, giá phòng và cọc
 * của riêng hợp đồng, rồi lưu và kích hoạt trong một lần. Giá phòng và cọc tự điền theo loại phòng
 * và tòa nhưng sửa được; số sửa chỉ áp cho hợp đồng này, bảng giá chung không đổi. Phòng, tiền cọc,
 * hóa đơn và các màn liên quan cập nhật theo vì cùng đọc từ phân phòng và sổ cọc.
 */
export default function ContractCreateModal({ open, onClose, onCreated }: Props) {
  const { message } = App.useApp()
  const queryClient = useQueryClient()
  const onError = useApiErrorHandler()

  const [applicationId, setApplicationId] = useState<number | undefined>()
  const [application, setApplication] = useState<CrudRecord | undefined>()
  const [filter, setFilter] = useState<{ buildingId?: number; roomTypeId?: number }>({})
  const [picked, setPicked] = useState<number | undefined>()
  const [startDate, setStartDate] = useState('')
  const [endDate, setEndDate] = useState('')
  const [priceMain, setPriceMain] = useState<number | null>(null)
  const [priceSummer, setPriceSummer] = useState<number | null>(null)
  const [activate, setActivate] = useState(true)
  const [paper, setPaper] = useState(false)
  const [deposit, setDeposit] = useState<number | null>(null)
  const [method, setMethod] = useState<string>(PaymentMethod.CASH)
  const [note, setNote] = useState('')
  const [errors, setErrors] = useState<Errors>({})

  const approved = useQuery({
    queryKey: ['crud-lookup', '/applications', { status: 'APPROVED' }],
    queryFn: () => getLookupRecords('/applications', { status: 'APPROVED' }),
    enabled: open,
    staleTime: STALE_MS,
  })
  const applicationOptions = (approved.data ?? [])
    .filter((row) => row.contractId == null)
    .map((row) => ({
      value: Number(row.id),
      label: `${String((row.student as CrudRecord | undefined)?.studentCode ?? '—')} · ${String(
        (row.student as CrudRecord | undefined)?.fullName ?? '—',
      )} — ${String(row.priorityScore ?? 0)} điểm ưu tiên`,
    }))

  const hasApplication = applicationId != null
  const beds = useAvailableBeds({ applicationId, ...filter }, open && hasApplication)
  const catalog = useAssignCatalog(
    open && hasApplication,
    application?.preferredUnitId as number | undefined,
  )
  const buildingOptions = (catalog.buildings.data ?? []).map((row) => ({
    value: Number(row.id),
    label: String(row.name ?? row.id),
  }))
  const roomTypeOptions = (catalog.roomTypes.data ?? []).map((row) => ({
    value: Number(row.id),
    label: String(row.name ?? row.id),
  }))

  // Giường đã chọn không còn trong danh sách mới thì coi như chưa chọn.
  const selected = useMemo(() => {
    for (const { room, freeBeds } of beds.data ?? []) {
      const bed = freeBeds.find((item) => item.id === picked)
      if (bed) {
        return {
          id: bed.id,
          label: formatBedCode(room.buildingCode, room.code, bed.code),
          priceMain: room.priceMain,
          roomTypeId: room.roomType.id,
          buildingId: room.buildingId,
        }
      }
    }
    return undefined
  }, [beds.data, picked])

  // Đổi giường thì điền lại giá và cọc theo loại phòng/tòa mới; người dùng sửa tiếp được.
  const selectedId = selected?.id
  useEffect(() => {
    if (!selected) return
    const roomType = (catalog.roomTypes.data ?? []).find(
      (row) => Number(row.id) === selected.roomTypeId,
    )
    const building = (catalog.buildings.data ?? []).find(
      (row) => Number(row.id) === selected.buildingId,
    )
    setPriceMain(selected.priceMain)
    setPriceSummer(typeof roomType?.priceSummer === 'number' ? roomType.priceSummer : null)
    setDeposit(typeof building?.depositAmount === 'number' ? building.depositAmount : null)
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [selectedId, catalog.roomTypes.data, catalog.buildings.data])

  const reset = useCallback(() => {
    setApplicationId(undefined)
    setApplication(undefined)
    setFilter({})
    setPicked(undefined)
    setStartDate('')
    setEndDate('')
    setPriceMain(null)
    setPriceSummer(null)
    setActivate(true)
    setPaper(false)
    setDeposit(null)
    setMethod(PaymentMethod.CASH)
    setNote('')
    setErrors({})
  }, [])

  const onSemester = useCallback((range: { start: string; end: string }) => {
    setStartDate((current) => current || range.start)
    setEndDate((current) => current || range.end)
  }, [])

  const save = useMutation({
    mutationFn: () =>
      assignApplicationContract(applicationId as number, {
        bedId: selected!.id,
        startDate,
        endDate,
        priceMain: priceMain ?? undefined,
        priceSummer: priceSummer ?? undefined,
        activate,
        ...(activate
          ? {
              depositAmount: deposit ?? undefined,
              paymentMethod: method,
              paperContractSigned: paper,
            }
          : {}),
        ...(note.trim() ? { note: note.trim() } : {}),
      }),
    onSuccess: () => {
      void queryClient.invalidateQueries()
      message.success(
        activate
          ? 'Đã tạo hợp đồng, giữ giường, thu cọc và kích hoạt.'
          : 'Đã tạo hợp đồng và giữ giường, chờ kích hoạt.',
      )
      reset()
      onCreated()
    },
    onError,
  })

  function validate() {
    const next: Errors = {}
    if (applicationId == null) next.applicationId = 'Chọn một đơn đã duyệt.'
    if (!selected) next.bed = 'Chọn một giường trống để giữ chỗ.'
    if (!startDate || !endDate) next.dates = 'Nhập đủ ngày bắt đầu và kết thúc.'
    else if (endDate <= startDate) next.dates = 'Ngày kết thúc phải sau ngày bắt đầu.'
    if (priceMain == null || priceMain < 0) next.priceMain = 'Nhập giá phòng học kỳ (từ 0 đ).'
    if (priceSummer == null || priceSummer < 0) next.priceSummer = 'Nhập giá phòng kỳ hè (từ 0 đ).'
    if (activate) {
      if (!paper) next.paper = 'Cần xác nhận đã ký giấy tờ hợp đồng với sinh viên.'
      if (deposit == null || deposit <= 0) next.deposit = 'Nhập số tiền cọc đã thu.'
    }
    if (note.trim().length > 500) next.note = 'Ghi chú tối đa 500 ký tự.'
    setErrors(next)
    return Object.keys(next).length === 0
  }

  function submit() {
    if (validate()) save.mutate()
  }

  return (
    <Modal
      title="Thêm hợp đồng"
      open={open}
      width={980}
      onCancel={() => {
        if (!save.isPending) {
          reset()
          onClose()
        }
      }}
      mask={{ closable: false }}
      footer={[
        <Button key="cancel" disabled={save.isPending} onClick={() => { reset(); onClose() }}>
          Đóng
        </Button>,
        <Button key="save" type="primary" loading={save.isPending} onClick={submit}>
          {activate ? 'Lưu và kích hoạt' : 'Lưu, chờ kích hoạt'}
        </Button>,
      ]}
    >
      <div style={{ display: 'grid', gap: 'var(--space-4)' }}>
        <FormField label="Đơn đã duyệt" required error={errors.applicationId}
          hint={approved.isLoading ? 'Đang tải danh sách đơn…' : approved.isError ? 'Không tải được danh sách đơn đã duyệt.' : applicationOptions.length === 0 ? 'Không có đơn đã duyệt nào chưa có hợp đồng.' : `Có ${applicationOptions.length} đơn đã duyệt chưa có hợp đồng.`}>
          {(control) => (
            <select
              {...control}
              className="dms-native-select"
              value={applicationId ?? ''}
              disabled={approved.isLoading}
              onChange={(event) => {
                const value = event.target.value
                setApplicationId(value === '' ? undefined : Number(value))
                setPicked(undefined)
                setFilter({})
                setErrors((current) => ({ ...current, applicationId: undefined }))
              }}
            >
              <option value="">Chọn đơn đã duyệt…</option>
              {applicationOptions.map((option) => (
                <option key={option.value} value={option.value}>
                  {option.label}
                </option>
              ))}
            </select>
          )}
        </FormField>

        <ContractApplicationPreview
          applicationId={applicationId}
          onSemester={onSemester}
          onApplication={setApplication}
        />

        {hasApplication ? (
          <>
            {beds.isError ? (
              <Alert type="error" showIcon title="Không tải được danh sách giường trống"
                description={errorMessage(beds.error)} />
            ) : null}
            <BedPicker
              title="Chọn giường"
              rooms={beds.data ?? []}
              loading={beds.isLoading}
              selectedBedId={selected?.id}
              onSelect={(id) => {
                setPicked(id)
                setErrors((current) => ({ ...current, bed: undefined }))
              }}
              buildings={buildingOptions}
              roomTypes={roomTypeOptions}
              buildingId={filter.buildingId}
              roomTypeId={filter.roomTypeId}
              onFilterChange={setFilter}
            />
            {errors.bed ? <div role="alert" className="dms-field-error">{errors.bed}</div> : null}

            <FormGrid>
              <FormField label="Ngày bắt đầu" required error={errors.dates}>
                {(control) => (
                  <Input {...control} type="date" value={startDate}
                    onChange={(event) => setStartDate(event.target.value)} />
                )}
              </FormField>
              <FormField label="Ngày kết thúc" required>
                {(control) => (
                  <Input {...control} type="date" value={endDate}
                    onChange={(event) => setEndDate(event.target.value)} />
                )}
              </FormField>
              <FormField label="Giá phòng / học kỳ" required error={errors.priceMain}
                hint={selected ? `Niêm yết theo loại phòng: ${formatMoney(selected.priceMain)}. Sửa chỉ áp cho hợp đồng này.` : 'Chọn giường để tự điền theo loại phòng.'}>
                {(control) => (
                  <InputNumber {...control} min={0} precision={0} style={{ width: '100%' }}
                    addonAfter="đ" value={priceMain}
                    onChange={(value) => setPriceMain(typeof value === 'number' ? value : null)} />
                )}
              </FormField>
              <FormField label="Giá phòng kỳ hè" required error={errors.priceSummer}>
                {(control) => (
                  <InputNumber {...control} min={0} precision={0} style={{ width: '100%' }}
                    addonAfter="đ" value={priceSummer}
                    onChange={(value) => setPriceSummer(typeof value === 'number' ? value : null)} />
                )}
              </FormField>
            </FormGrid>

            <Checkbox checked={activate} onChange={(event) => setActivate(event.target.checked)}>
              Thu cọc và kích hoạt hợp đồng ngay (bỏ chọn để chỉ giữ giường, kích hoạt sau)
            </Checkbox>

            {activate ? (
              <FormGrid>
                <div className="dms-form-span-full">
                  <Checkbox checked={paper} onChange={(event) => {
                    setPaper(event.target.checked)
                    setErrors((current) => ({ ...current, paper: undefined }))
                  }}>
                    Đã ký giấy tờ hợp đồng với sinh viên và đối chiếu thông tin.
                  </Checkbox>
                  {errors.paper ? <div role="alert" className="dms-field-error">{errors.paper}</div> : null}
                </div>
                <FormField label="Số tiền cọc đã thu" required error={errors.deposit}
                  hint="Tự điền theo mức cọc của tòa; sửa chỉ áp cho hợp đồng này.">
                  {(control) => (
                    <InputNumber {...control} min={1} precision={0} style={{ width: '100%' }}
                      addonAfter="đ" value={deposit}
                      onChange={(value) => setDeposit(typeof value === 'number' ? value : null)} />
                  )}
                </FormField>
                <FormField label="Hình thức thu cọc" required>
                  {() => (
                    <RadioGroup value={method} onChange={(event) => setMethod(event.target.value)}>
                      <Radio value={PaymentMethod.CASH}>Tiền mặt</Radio>
                      <Radio value={PaymentMethod.TRANSFER}>Chuyển khoản</Radio>
                    </RadioGroup>
                  )}
                </FormField>
              </FormGrid>
            ) : null}

            <FormField label="Ghi chú" error={errors.note}>
              {(control) => (
                <TextArea {...control} rows={2} maxLength={500} value={note}
                  onChange={(event) => setNote(event.target.value)} />
              )}
            </FormField>
          </>
        ) : null}
      </div>
    </Modal>
  )
}
