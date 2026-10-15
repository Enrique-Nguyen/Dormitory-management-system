import { SearchOutlined } from '@ant-design/icons'
import {
  Checkbox as AntCheckbox,
  DatePicker as AntDatePicker,
  Input as AntInput,
  InputNumber as AntInputNumber,
  Radio as AntRadio,
  Select as AntSelect,
  Switch as AntSwitch,
  type SelectProps,
} from 'antd'
import type {
  BaseOptionType,
  DefaultOptionType,
  RefSelectProps,
} from 'antd/es/select'
import type { ComponentPropsWithRef, Ref } from 'react'
import { commonUiLabels } from '../../constants/navigation'

/** Chuyển nguyên value/onChange/ref và aria-* để Ant Form và React Hook Form cùng dùng được. */
export function Input({
  className = '',
  ...props
}: ComponentPropsWithRef<typeof AntInput>) {
  return <AntInput {...props} className={`dms-input ${className}`} />
}

export function PasswordInput({
  className = '',
  ...props
}: ComponentPropsWithRef<typeof AntInput.Password>) {
  return <AntInput.Password {...props} className={`dms-input-password ${className}`} />
}

export function TextArea({
  className = '',
  ...props
}: ComponentPropsWithRef<typeof AntInput.TextArea>) {
  return (
    <AntInput.TextArea {...props} className={`dms-textarea ${className}`} />
  )
}

/** Luôn cho phép xóa từ khóa; caller tiếp tục quyết định lúc nào gọi API tìm kiếm. */
export function SearchInput({
  className = '',
  ...props
}: ComponentPropsWithRef<typeof AntInput.Search>) {
  return (
    <AntInput.Search
      allowClear
      aria-label={commonUiLabels.search}
      enterButton={<SearchOutlined aria-label={commonUiLabels.search} />}
      {...props}
      className={`dms-search ${className}`}
    />
  )
}

export function InputNumber({
  className = '',
  ...props
}: ComponentPropsWithRef<typeof AntInputNumber>) {
  return (
    <AntInputNumber {...props} className={`dms-number-input ${className}`} />
  )
}

/** Giữ generic của Select để value/onChange vẫn đúng kiểu khi chọn đơn hoặc nhiều giá trị. */
export function Select<
  ValueType = unknown,
  OptionType extends BaseOptionType | DefaultOptionType = DefaultOptionType,
>({
  className = '',
  ...props
}: SelectProps<ValueType, OptionType> & { ref?: Ref<RefSelectProps> }) {
  return (
    <AntSelect<ValueType, OptionType>
      {...props}
      className={`dms-select ${className}`}
    />
  )
}

export function Switch({
  className = '',
  ...props
}: ComponentPropsWithRef<typeof AntSwitch>) {
  return <AntSwitch {...props} className={`dms-switch ${className}`} />
}

export function Checkbox({
  className = '',
  ...props
}: ComponentPropsWithRef<typeof AntCheckbox>) {
  return <AntCheckbox {...props} className={`dms-checkbox ${className}`} />
}

export function Radio({
  className = '',
  ...props
}: ComponentPropsWithRef<typeof AntRadio>) {
  return <AntRadio {...props} className={`dms-radio ${className}`} />
}

export function RadioGroup({
  className = '',
  ...props
}: ComponentPropsWithRef<typeof AntRadio.Group>) {
  return <AntRadio.Group {...props} className={`dms-radio-group ${className}`} />
}

export function DatePicker({
  className = '',
  format = 'DD/MM/YYYY',
  placeholder = 'Chọn ngày',
  ...props
}: ComponentPropsWithRef<typeof AntDatePicker>) {
  return (
    <AntDatePicker
      format={format}
      placeholder={placeholder}
      {...props}
      className={`dms-datepicker ${className}`}
    />
  )
}

export function RangePicker({
  className = '',
  format = 'DD/MM/YYYY',
  placeholder = ['Từ ngày', 'Đến ngày'],
  ...props
}: ComponentPropsWithRef<typeof AntDatePicker.RangePicker>) {
  return (
    <AntDatePicker.RangePicker
      format={format}
      placeholder={placeholder}
      {...props}
      className={`dms-rangepicker ${className}`}
    />
  )
}
