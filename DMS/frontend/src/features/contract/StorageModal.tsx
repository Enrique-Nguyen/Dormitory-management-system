import { App } from 'antd'
import dayjs, { type Dayjs } from 'dayjs'
import { useMemo, useState } from 'react'
import { Controller, useForm } from 'react-hook-form'
import { useQuery } from '@tanstack/react-query'
import { getContract } from '../../services/api/contract'
import { ContractStatus } from '../../constants/statusConfig'
import ReferenceSelect from '../crud/ReferenceSelect'
import {
  Alert,
  BedPicker,
  Button,
  DatePicker,
  Descriptions,
  FormField,
  FormGrid,
  InputNumber,
  Modal,
  TextArea,
} from '../../components'
import type { CrudRecord } from '../crud/types'
import type { StorageRecordItem } from '../../services/api/checkout'
import { errorMessage } from '../../utils/errorMessages'
import { formatBedCode, formatDate } from '../../utils/format'
import { useRoomCatalog } from '../room'
import {
  useCreateStorageRecord,
  useReturnStorageRecord,
  useStorageAvailableBeds,
  type StorageBedFilter,
} from './useCheckout'

export type StorageModalMode = 'create' | 'return'

export interface StorageModalProps {
  mode: StorageModalMode
  open: boolean
  onClose: () => void
  /** Hợp đồng ACTIVE khi tạo phiếu; có thể nhập tay nếu mở từ trang kho. */
  contractId?: number
  contractCode?: string
  managementUnitId?: number
  /** Phiếu cần nhận lại đồ ở chế độ `return`. */
  record?: StorageRecordItem | null
  onSuccess?: (record: StorageRecordItem) => void
}

interface CreateFormState {
  contractId: number | null
  packageCount: number | null
  description: string
  depositedAt: Dayjs
}

type CreateErrors = Partial<
  Record<'contractId' | 'packageCount' | 'description' | 'depositedAt', string>
>

interface ReturnErrors {
  bedId?: string
  returnedAt?: string
}

function toOptions(records: CrudRecord[] | undefined, labelKey: string) {
  return (records ?? []).map((record) => ({
    value: Number(record.id),
    label: String(record[labelKey] ?? record.id),
  }))
}

function CreateStorageModal({
  open,
  contractId,
  contractCode,
  managementUnitId,
  onClose,
  onSuccess,
}: Pick<
  StorageModalProps,
  'open' | 'contractId' | 'contractCode' | 'managementUnitId' | 'onClose' | 'onSuccess'
>) {
  const { message } = App.useApp()
  const createStorage = useCreateStorageRecord()
  const [form, setForm] = useState<CreateFormState>(() => ({
    contractId: contractId ?? null,
    packageCount: 1,
    description: '',
    depositedAt: dayjs(),
  }))
  const [errors, setErrors] = useState<CreateErrors>({})
  const contract = useQuery({
    queryKey: ['storage-contract', form.contractId],
    queryFn: () => getContract(form.contractId!),
    enabled: open && form.contractId != null,
  })
  const unitId = contract.data?.currentAssignment?.bed?.unitId ?? managementUnitId
  const { control, reset, trigger, getValues, formState: { errors: referenceErrors } } = useForm<{
    locationId: number | undefined
  }>({ defaultValues: { locationId: undefined } })

  function resetForm() {
    setForm({
      contractId: contractId ?? null,
      packageCount: 1,
      description: '',
      depositedAt: dayjs(),
    })
    reset({ locationId: undefined })
    setErrors({})
  }

  function validate() {
    const next: CreateErrors = {}
    if (!form.contractId || !Number.isSafeInteger(form.contractId))
      next.contractId = 'Chọn hợp đồng đang ACTIVE.'
    if (!form.packageCount || form.packageCount < 1)
      next.packageCount = 'Số kiện phải từ 1 đến 32767.'
    if (form.packageCount && form.packageCount > 32767)
      next.packageCount = 'Số kiện phải từ 1 đến 32767.'
    if (!form.description.trim())
      next.description = 'Mô tả các kiện đồ gửi.'
    if (!form.depositedAt || !form.depositedAt.isValid())
      next.depositedAt = 'Chọn ngày gửi đồ.'
    setErrors(next)
    return Object.keys(next).length === 0
  }

  async function submit() {
    const validReference = await trigger('locationId')
    if (!validate() || !validReference || !form.contractId || unitId == null || contract.isError || contract.isFetching) return
    try {
      const record = await createStorage.mutateAsync({
        contractId: form.contractId,
        packageCount: form.packageCount!,
        description: form.description.trim(),
        locationId: getValues('locationId')!,
        depositedAt: form.depositedAt.format('YYYY-MM-DD'),
      })
      message.success('Đã tạo phiếu gửi đồ.')
      onSuccess?.(record)
      onClose()
    } catch {
      // useApiErrorHandler đã hiện câu tiếng Việt; giữ hộp thoại để sửa dữ liệu.
    }
  }

  return (
    <Modal
      title="Tạo phiếu gửi đồ"
      open={open}
      onCancel={() => {
        if (!createStorage.isPending) onClose()
      }}
      footer={[
        <Button
          key="cancel"
          onClick={onClose}
          disabled={createStorage.isPending}
        >
          Hủy
        </Button>,
        <Button
          key="submit"
          type="primary"
          loading={createStorage.isPending}
          onClick={() => void submit()}
        >
          Tạo phiếu
        </Button>,
      ]}
      width={760}
      mask={{ closable: false }}
      keyboard={!createStorage.isPending}
      closable={!createStorage.isPending}
      afterOpenChange={(visible) => {
        if (visible) resetForm()
      }}
    >
      <Alert
        type="info"
        showIcon
        style={{ marginBottom: 16 }}
        title="Chỉ áp dụng cho hợp đồng đang ACTIVE"
        description="Sinh viên phải có lựa chọn tạm rời hè đã duyệt và hợp đồng đã được gia hạn qua học kỳ hè. Tạo phiếu sẽ đóng phân phòng hiện tại nhưng giữ nguyên hợp đồng và tiền cọc."
      />

      <FormGrid>
        <FormField
          label="Mã hợp đồng"
          required
          error={errors.contractId}
          hint={
            contractId
              ? 'Hợp đồng được xác định từ ngữ cảnh đang mở.'
              : 'Chọn hợp đồng đang ACTIVE của sinh viên gửi đồ.'
          }
        >
          {(control) =>
            contractId ? (
              <div id={control.id} style={{ fontWeight: 600 }}>
                {contractCode ?? `#${contractId}`}
              </div>
            ) : (
              <ReferenceSelect
                {...control}
                endpoint="/contracts"
                labelField="code"
                filters={{ status: ContractStatus.ACTIVE }}
                value={form.contractId ?? undefined}
                allowClear
                placeholder="Chọn hợp đồng"
                onChange={(value) => {
                  reset({ locationId: undefined })
                  setForm((current) => ({ ...current, contractId: typeof value === 'number' ? value : null }))
                }}
              />
            )
          }
        </FormField>

        <FormField
          label="Số kiện đồ"
          required
          error={errors.packageCount}
        >
          {(control) => (
            <InputNumber
              {...control}
              min={1}
              max={32767}
              precision={0}
              style={{ width: '100%' }}
              value={form.packageCount}
              onChange={(value) =>
                setForm((current) => ({
                  ...current,
                  packageCount: typeof value === 'number' ? value : null,
                }))
              }
            />
          )}
        </FormField>

        <FormField
          label="Ngày gửi đồ"
          required
          error={errors.depositedAt}
        >
          {(control) => (
            <DatePicker
              {...control}
              style={{ width: '100%' }}
              value={form.depositedAt}
              onChange={(value) => {
                if (value)
                  setForm((current) => ({
                    ...current,
                    depositedAt: value as Dayjs,
                  }))
              }}
            />
          )}
        </FormField>

        <FormField
          label="Vị trí lưu kho"
          required
          error={referenceErrors.locationId?.message}
        >
          {(fieldControl) => (
            <Controller
              name="locationId"
              control={control}
              rules={{ required: 'Chọn vị trí lưu kho trong danh mục.' }}
              render={({ field }) => (
                <ReferenceSelect
                  {...fieldControl}
                  endpoint="/storage-locations"
                  filters={{ active: true, ...(unitId != null ? { managementUnitId: unitId } : {}) }}
                  value={field.value}
                  onChange={(value) => field.onChange(typeof value === 'number' ? value : undefined)}
                  disabled={createStorage.isPending || unitId == null || contract.isFetching || contract.isError}
                  placeholder={unitId == null ? 'Chọn hợp đồng có phân phòng trước' : 'Chọn vị trí lưu kho'}
                />
              )}
            />
          )}
        </FormField>

        <div className="dms-form-span-full">
          <FormField
            label="Mô tả đồ gửi"
            required
            error={errors.description}
            hint="Tối đa 500 ký tự."
          >
            {(control) => (
              <TextArea
                {...control}
                rows={3}
                maxLength={500}
                showCount
                placeholder="Ví dụ: 2 thùng sách, 1 vali quần áo..."
                value={form.description}
                onChange={(event) =>
                  setForm((current) => ({
                    ...current,
                    description: event.target.value,
                  }))
                }
              />
            )}
          </FormField>
        </div>
      </FormGrid>
    </Modal>
  )
}

function ReturnStorageModal({
  open,
  record,
  onClose,
  onSuccess,
}: Pick<StorageModalProps, 'open' | 'record' | 'onClose' | 'onSuccess'>) {
  const { message } = App.useApp()
  const [filter, setFilter] = useState<StorageBedFilter>({})
  const [picked, setPicked] = useState<number | undefined>()
  const [returnedAt, setReturnedAt] = useState<Dayjs>(() => {
    const today = dayjs()
    const deposited = record ? dayjs(record.depositedAt) : today
    return today.isBefore(deposited, 'day') ? deposited : today
  })
  const [errors, setErrors] = useState<ReturnErrors>({})
  const beds = useStorageAvailableBeds(record?.id, filter, open)
  const buildings = useRoomCatalog('/buildings')
  const roomTypes = useRoomCatalog('/room-types')
  const returnStorage = useReturnStorageRecord()

  // Giường đã chọn không còn trong danh sách mới (vừa bị lấy, đổi bộ lọc) thì coi như chưa chọn.
  const selected = useMemo(() => {
    for (const { room, freeBeds } of beds.data ?? []) {
      const bed = freeBeds.find((item) => item.id === picked)
      if (bed)
        return {
          id: bed.id,
          label: formatBedCode(room.buildingCode, room.code, bed.code),
        }
    }
    return undefined
  }, [beds.data, picked])

  function resetReturn() {
    setPicked(undefined)
    setFilter({})
    setErrors({})
    const deposited = record ? dayjs(record.depositedAt) : dayjs()
    const today = dayjs()
    setReturnedAt(today.isBefore(deposited, 'day') ? deposited : today)
  }

  function validate() {
    const next: ReturnErrors = {}
    if (!selected) next.bedId = 'Chọn giường để nhận lại đồ.'
    if (!returnedAt || !returnedAt.isValid())
      next.returnedAt = 'Chọn ngày nhận lại đồ.'
    else if (record && returnedAt.isBefore(dayjs(record.depositedAt), 'day'))
      next.returnedAt = 'Ngày nhận lại đồ không trước ngày gửi.'
    setErrors(next)
    return Object.keys(next).length === 0
  }

  async function submit() {
    if (!record || !validate() || !selected) return
    try {
      const result = await returnStorage.mutateAsync({
        storageRecordId: record.id,
        bedId: selected.id,
        returnedAt: returnedAt.format('YYYY-MM-DD'),
      })
      message.success(
        `Đã nhận lại đồ${record.fullName ? ` của ${record.fullName}` : ''} và phân giường ${selected.label}.`,
      )
      onSuccess?.(result)
      onClose()
    } catch {
      // useApiErrorHandler đã hiện câu tiếng Việt; giữ hộp thoại để chọn giường khác.
    }
  }

  return (
    <Modal
      title={`Nhận lại đồ — phiếu #${record?.id ?? ''}`}
      open={open}
      onCancel={() => {
        if (!returnStorage.isPending) onClose()
      }}
      footer={[
        <Button
          key="cancel"
          onClick={onClose}
          disabled={returnStorage.isPending}
        >
          Hủy
        </Button>,
        <Button
          key="submit"
          type="primary"
          loading={returnStorage.isPending}
          disabled={!record || !selected}
          onClick={() => void submit()}
        >
          Xác nhận nhận đồ
        </Button>,
      ]}
      width={960}
      mask={{ closable: false }}
      keyboard={!returnStorage.isPending}
      closable={!returnStorage.isPending}
      afterOpenChange={(visible) => {
        if (visible) resetReturn()
      }}
    >
      {record && (
        <Descriptions
          bordered
          size="small"
          column={{ xs: 1, sm: 2 }}
          style={{ marginBottom: 16 }}
          items={[
            {
              key: 'student',
              label: 'Sinh viên',
              children: record.fullName ?? '—',
            },
            {
              key: 'contract',
              label: 'Hợp đồng',
              children: record.contractCode ?? `#${record.contractId}`,
            },
            {
              key: 'packages',
              label: 'Số kiện',
              children: record.packageCount,
            },
            {
              key: 'depositedAt',
              label: 'Ngày gửi',
              children: formatDate(record.depositedAt),
            },
            {
              key: 'description',
              label: 'Mô tả',
              children: record.description,
            },
            {
              key: 'location',
              label: 'Vị trí',
              children: record.location,
            },
          ]}
        />
      )}

      <div style={{ maxWidth: 420 }}>
        <FormField
          label="Ngày nhận lại đồ"
          required
          error={errors.returnedAt}
        >
          {(control) => (
            <DatePicker
              {...control}
              style={{ width: '100%' }}
              value={returnedAt}
              onChange={(value) => {
                if (value) setReturnedAt(value as Dayjs)
              }}
            />
          )}
        </FormField>
      </div>

      <Alert
        type="info"
        showIcon
        style={{ margin: '16px 0' }}
        title="Chọn giường trống để phân lại chỗ ở"
        description="Danh sách giường được lọc theo đơn vị, giới tính và trạng thái phòng. Có thể thu hẹp theo tòa hoặc loại phòng."
      />

      {beds.isError ? (
        <Alert
          type="error"
          showIcon
          title="Không thể tải danh sách giường trống"
          description={errorMessage(beds.error)}
        />
      ) : (
        <BedPicker
          title="Giường trống có thể nhận"
          rooms={beds.data ?? []}
          loading={beds.isLoading}
          selectedBedId={selected?.id}
          onSelect={setPicked}
          buildings={toOptions(buildings.data, 'name')}
          roomTypes={toOptions(roomTypes.data, 'name')}
          buildingId={filter.buildingId}
          roomTypeId={filter.roomTypeId}
          onFilterChange={setFilter}
        />
      )}

      {errors.bedId && (
        <div role="alert" className="dms-field-error" style={{ marginTop: 8 }}>
          {errors.bedId}
        </div>
      )}

      {selected && (
        <Alert
          type="success"
          showIcon
          style={{ marginTop: 16 }}
          title={`Đã chọn giường ${selected.label}`}
        />
      )}
    </Modal>
  )
}

/**
 * Hộp thoại gửi đồ/nhận lại đồ (API 4.9). Chế độ `create` thu thập kiện đồ và ngày gửi; chế độ
 * `return` nhận phiếu đang mở, chọn giường trống qua BedPicker và ghi ngày nhận. Điều kiện nghiệp
 * vụ (hợp đồng ACTIVE, TEMP_LEAVE đã duyệt, đã gia hạn) do backend kiểm tra lại.
 */
export default function StorageModal(props: StorageModalProps) {
  return props.mode === 'create' ? (
    <CreateStorageModal
      open={props.open}
      contractId={props.contractId}
      contractCode={props.contractCode}
      managementUnitId={props.managementUnitId}
      onClose={props.onClose}
      onSuccess={props.onSuccess}
    />
  ) : (
    <ReturnStorageModal
      open={props.open}
      record={props.record}
      onClose={props.onClose}
      onSuccess={props.onSuccess}
    />
  )
}
