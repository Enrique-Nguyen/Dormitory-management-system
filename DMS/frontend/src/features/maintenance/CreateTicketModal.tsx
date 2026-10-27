// oxlint-disable react/set-state-in-effect -- modal mở lại phải reset form/ảnh đã chọn
import { App } from 'antd'
import { useEffect, useId, useMemo, useState } from 'react'
import { Controller, useForm, useWatch } from 'react-hook-form'
import {
  Alert,
  Button,
  FormField,
  FormGrid,
  InputNumber,
  Modal,
  Radio,
  RadioGroup,
  Select,
  TextArea,
} from '../../components'
import { Role } from '../../constants/authorization'
import type {
  MaintenanceDetail,
  MaintenanceSaveInput,
} from '../../services/api/maintenance'
import { errorMessage } from '../../utils/errorMessages'
import { useMe } from '../account/useMe'
import {
  maintenanceCategoryOptions,
  maintenancePhotoRules,
  maintenanceSeverityOptions,
} from './constants'
import {
  useCreateTicket,
  useMaintenanceBuildings,
  useMaintenanceRooms,
} from './useMaintenance'

interface Props {
  open: boolean
  onClose: () => void
  /** Màn gọi có thể điều hướng tới ticket vừa tạo. */
  onCreated?: (ticket: MaintenanceDetail) => void
}

type LocationType = 'ROOM' | 'COMMON'

interface CreateTicketForm {
  category: string
  severity: string
  description: string
  locationType: LocationType
  roomId?: number
  buildingId?: number
  floor?: number
}

const emptyForm: CreateTicketForm = {
  category: '',
  severity: '',
  description: '',
  locationType: 'ROOM',
}

function asNumber(value: unknown): number {
  const number = typeof value === 'number' ? value : Number(value)
  return Number.isFinite(number) ? number : 0
}

function asText(value: unknown): string {
  return typeof value === 'string' ? value : String(value ?? '')
}

/**
 * Báo hỏng dùng chung cho Sinh viên và Quản lý (FR-MNT-01, API 4.11).
 * Sinh viên không gửi vị trí (backend tự lấy phòng đang ở); Quản lý chọn đúng một
 * phòng HOẶC khu vực chung gồm tòa + tầng. Ảnh gửi multipart cùng JSON `data`.
 */
export default function CreateTicketModal({
  open,
  onClose,
  onCreated,
}: Props) {
  const { message } = App.useApp()
  const { data: me } = useMe()
  const {
    control,
    handleSubmit,
    reset,
    setValue,
    setError,
    clearErrors,
    formState: { errors },
  } = useForm<CreateTicketForm>({ defaultValues: emptyForm })

  const [photos, setPhotos] = useState<File[]>([])
  const [photoError, setPhotoError] = useState<string>()
  const [previews, setPreviews] = useState<string[]>([])
  const fileInputId = useId()
  const photoHintId = `${fileInputId}-hint`

  const canChooseLocation =
    me?.role === Role.STAFF || me?.role === Role.ADMIN
  const locationType = useWatch({ control, name: 'locationType' })
  const create = useCreateTicket()
  const rooms = useMaintenanceRooms(open && canChooseLocation)
  const buildings = useMaintenanceBuildings(open && canChooseLocation)

  useEffect(() => {
    if (!open) return
    reset(emptyForm)
    setPhotos([])
    setPhotoError(undefined)
  }, [open, reset])

  // Blob URL chỉ sống theo danh sách ảnh đang chọn và được thu hồi khi đổi/đóng.
  useEffect(() => {
    const urls = photos.map((file) => URL.createObjectURL(file))
    setPreviews(urls)
    return () => {
      for (const url of urls) URL.revokeObjectURL(url)
    }
  }, [photos])

  const roomOptions = useMemo(() => {
    const buildingLabels = new Map<number, string>()
    for (const row of buildings.data ?? []) {
      const id = asNumber(row.id)
      if (id <= 0) continue
      const code = asText(row.code)
      const name = asText(row.name)
      buildingLabels.set(id, name ? `${code} – ${name}` : code)
    }
    return (rooms.data ?? [])
      .map((row) => {
        const id = asNumber(row.id)
        const buildingId = asNumber(row.buildingId)
        const floor = asNumber(row.floor)
        const building = buildingLabels.get(buildingId) ?? `Tòa #${buildingId}`
        return {
          value: id,
          label: `${asText(row.code)} · ${building} · Tầng ${floor}`,
        }
      })
      .filter((option) => option.value > 0)
  }, [rooms.data, buildings.data])

  const buildingOptions = useMemo(
    () =>
      (buildings.data ?? [])
        .map((row) => {
          const id = asNumber(row.id)
          const code = asText(row.code)
          const name = asText(row.name)
          return { value: id, label: name ? `${code} – ${name}` : code }
        })
        .filter((option) => option.value > 0),
    [buildings.data],
  )

  function addPhotos(fileList: FileList | null) {
    if (!fileList || fileList.length === 0) return
    const allowedTypes: readonly string[] = maintenancePhotoRules.acceptedTypes
    const accepted: File[] = []
    let nextError: string | undefined
    for (const file of Array.from(fileList)) {
      if (!allowedTypes.includes(file.type)) {
        nextError = `Ảnh ${file.name} không phải JPEG, PNG hoặc WebP.`
        continue
      }
      if (file.size > maintenancePhotoRules.maxBytes) {
        nextError = `Ảnh ${file.name} vượt quá 5 MB.`
        continue
      }
      accepted.push(file)
    }
    const combined = [...photos, ...accepted]
    if (combined.length > maintenancePhotoRules.maxFiles) {
      nextError = `Chỉ đính kèm tối đa ${maintenancePhotoRules.maxFiles} ảnh.`
    }
    setPhotos(combined.slice(0, maintenancePhotoRules.maxFiles))
    setPhotoError(nextError)
  }

  function removePhoto(index: number) {
    setPhotos((current) => current.filter((_, position) => position !== index))
    setPhotoError(undefined)
  }

  const submit = handleSubmit(async (values) => {
    if (canChooseLocation) {
      if (values.locationType === 'ROOM' && values.roomId == null) {
        setError('roomId', { message: 'Chọn phòng xảy ra sự cố.' })
        return
      }
      if (values.locationType === 'COMMON') {
        if (values.buildingId == null) {
          setError('buildingId', { message: 'Chọn tòa nhà của khu vực chung.' })
          return
        }
        if (values.floor == null) {
          setError('floor', { message: 'Nhập tầng của khu vực chung.' })
          return
        }
      }
    }

    const data: Omit<MaintenanceSaveInput, 'id'> = {
      category: values.category,
      severity: values.severity,
      description: values.description.trim(),
    }
    if (canChooseLocation) {
      if (values.locationType === 'ROOM') data.roomId = values.roomId
      else {
        data.buildingId = values.buildingId
        data.floor = values.floor
      }
    }

    try {
      const ticket = await create.mutateAsync({ data, photos })
      void message.success(`Đã gửi yêu cầu sửa chữa ${ticket.code}.`)
      onClose()
      onCreated?.(ticket)
    } catch {
      // useApiErrorHandler đã hiện câu lỗi; giữ hộp thoại để người dùng sửa.
    }
  })

  return (
    <Modal
      title="Báo hỏng cơ sở vật chất"
      open={open}
      width={720}
      onCancel={onClose}
      onOk={() => void submit()}
      confirmLoading={create.isPending}
      okText="Gửi yêu cầu"
      cancelText="Hủy"
      closable={!create.isPending}
      mask={{ closable: false }}
      cancelButtonProps={{ disabled: create.isPending }}
      okButtonProps={{ disabled: !me }}
      destroyOnHidden
    >
      <Alert
        type="info"
        showIcon
        style={{ marginBottom: 'var(--space-4)' }}
        title="Mô tả càng rõ càng giúp kỹ thuật viên xử lý nhanh"
        description={
          canChooseLocation
            ? 'Chọn một phòng cụ thể hoặc một khu vực chung (tòa và tầng), không chọn cả hai.'
            : 'Yêu cầu sẽ được gắn với phòng bạn đang ở tại thời điểm gửi.'
        }
      />

      <FormGrid>
        <FormField label="Hạng mục" required error={errors.category?.message}>
          {(fieldControl) => (
            <Controller
              name="category"
              control={control}
              rules={{ required: 'Chọn hạng mục sự cố.' }}
              render={({ field }) => (
                <Select
                  {...field}
                  {...fieldControl}
                  options={[...maintenanceCategoryOptions]}
                  placeholder="Chọn hạng mục"
                />
              )}
            />
          )}
        </FormField>

        <FormField label="Mức độ" required error={errors.severity?.message}>
          {(fieldControl) => (
            <Controller
              name="severity"
              control={control}
              rules={{ required: 'Chọn mức độ sự cố.' }}
              render={({ field }) => (
                <Select
                  {...field}
                  {...fieldControl}
                  options={[...maintenanceSeverityOptions]}
                  placeholder="Chọn mức độ"
                />
              )}
            />
          )}
        </FormField>

        <div className="dms-form-span-full">
          <FormField
            label="Mô tả sự cố"
            required
            error={errors.description?.message}
          >
            {(fieldControl) => (
              <Controller
                name="description"
                control={control}
                rules={{
                  required: 'Nhập mô tả sự cố.',
                  validate: (value) =>
                    value.trim().length > 0 || 'Nhập mô tả sự cố.',
                }}
                render={({ field }) => (
                  <TextArea
                    {...field}
                    {...fieldControl}
                    rows={4}
                    maxLength={1000}
                    showCount
                    placeholder="Ví dụ: vòi nước trong phòng vệ sinh bị rò rỉ từ sáng nay..."
                  />
                )}
              />
            )}
          </FormField>
        </div>

        {canChooseLocation ? (
          <>
            <div className="dms-form-span-full">
              <FormField label="Vị trí sự cố" required>
                {(fieldControl) => (
                  <Controller
                    name="locationType"
                    control={control}
                    render={({ field }) => (
                      <RadioGroup
                        {...fieldControl}
                        value={field.value}
                        onChange={(event) => {
                          const next = event.target.value as LocationType
                          field.onChange(next)
                          if (next === 'ROOM') {
                            setValue('buildingId', undefined)
                            setValue('floor', undefined)
                          } else {
                            setValue('roomId', undefined)
                          }
                          clearErrors('roomId')
                          clearErrors('buildingId')
                          clearErrors('floor')
                        }}
                      >
                        <Radio value="ROOM">Phòng cụ thể</Radio>
                        <Radio value="COMMON">Khu vực chung</Radio>
                      </RadioGroup>
                    )}
                  />
                )}
              </FormField>
            </div>

            {locationType === 'ROOM' ? (
              <div className="dms-form-span-full">
                <FormField
                  label="Phòng"
                  required
                  error={errors.roomId?.message}
                  hint="Chỉ hiện các phòng thuộc đơn vị quản lý của bạn."
                >
                  {(fieldControl) => (
                    <Controller
                      name="roomId"
                      control={control}
                      rules={{ required: 'Chọn phòng xảy ra sự cố.' }}
                      render={({ field }) => (
                        <Select
                          {...field}
                          {...fieldControl}
                          options={roomOptions}
                          loading={rooms.isLoading}
                          disabled={rooms.isError}
                          showSearch
                          optionFilterProp="label"
                          allowClear
                          placeholder="Tìm theo mã phòng"
                          notFoundContent={
                            rooms.isLoading
                              ? 'Đang tải danh sách phòng...'
                              : 'Không có phòng phù hợp'
                          }
                        />
                      )}
                    />
                  )}
                </FormField>
                {rooms.isError ? (
                  <Alert
                    type="error"
                    showIcon
                    title="Không tải được danh sách phòng"
                    description={errorMessage(rooms.error)}
                  />
                ) : null}
              </div>
            ) : (
              <>
                <FormField
                  label="Tòa nhà"
                  required
                  error={errors.buildingId?.message}
                >
                  {(fieldControl) => (
                    <Controller
                      name="buildingId"
                      control={control}
                      rules={{ required: 'Chọn tòa nhà.' }}
                      render={({ field }) => (
                        <Select
                          {...field}
                          {...fieldControl}
                          options={buildingOptions}
                          loading={buildings.isLoading}
                          disabled={buildings.isError}
                          showSearch
                          optionFilterProp="label"
                          allowClear
                          placeholder="Chọn tòa"
                        />
                      )}
                    />
                  )}
                </FormField>
                <FormField label="Tầng" required error={errors.floor?.message}>
                  {(fieldControl) => (
                    <Controller
                      name="floor"
                      control={control}
                      rules={{ required: 'Nhập tầng.' }}
                      render={({ field }) => (
                        <InputNumber
                          {...field}
                          {...fieldControl}
                          min={0}
                          max={32767}
                          precision={0}
                          style={{ width: '100%' }}
                          placeholder="Ví dụ: 2"
                          onChange={(value) =>
                            field.onChange(
                              typeof value === 'number' ? value : undefined,
                            )
                          }
                        />
                      )}
                    />
                  )}
                </FormField>
                {buildings.isError ? (
                  <div className="dms-form-span-full">
                    <Alert
                      type="error"
                      showIcon
                      title="Không tải được danh sách tòa nhà"
                      description={errorMessage(buildings.error)}
                    />
                  </div>
                ) : null}
              </>
            )}
          </>
        ) : null}
      </FormGrid>

      <div className="dms-field">
        <label className="dms-field-label" htmlFor={fileInputId}>
          Ảnh hiện trường
        </label>
        <input
          id={fileInputId}
          type="file"
          multiple
          accept={maintenancePhotoRules.acceptedTypes.join(',')}
          aria-describedby={photoHintId}
          onChange={(event) => {
            addPhotos(event.target.files)
            // Cho phép chọn lại đúng tệp vừa xóa.
            event.target.value = ''
          }}
          style={{ width: '100%' }}
        />
        <div id={photoHintId} className="dms-field-hint">
          Tối đa {maintenancePhotoRules.maxFiles} ảnh JPEG, PNG hoặc WebP, mỗi
          ảnh không quá {maintenancePhotoRules.maxBytes / (1024 * 1024)} MB. Ảnh
          được tải cùng lúc với yêu cầu.
        </div>
        {photoError ? (
          <div role="alert" className="dms-field-error">
            {photoError}
          </div>
        ) : null}
        {photos.length > 0 ? (
          <div
            style={{
              display: 'grid',
              gridTemplateColumns: 'repeat(auto-fill, minmax(120px, 1fr))',
              gap: 'var(--space-3)',
              marginTop: 'var(--space-3)',
            }}
          >
            {photos.map((file, index) => (
              <div key={`${file.name}-${file.lastModified}-${index}`}>
                {previews[index] ? (
                  <img
                    src={previews[index]}
                    alt={`Ảnh đính kèm ${index + 1}`}
                    style={{
                      display: 'block',
                      width: '100%',
                      aspectRatio: '4 / 3',
                      objectFit: 'cover',
                      border: '1px solid var(--color-border)',
                      borderRadius: 'var(--radius-md)',
                      background: 'var(--color-bg-alt)',
                    }}
                  />
                ) : null}
                <Button
                  size="small"
                  danger
                  style={{ marginTop: 'var(--space-2)', width: '100%' }}
                  onClick={() => removePhoto(index)}
                >
                  Xóa ảnh
                </Button>
              </div>
            ))}
          </div>
        ) : null}
      </div>
    </Modal>
  )
}
