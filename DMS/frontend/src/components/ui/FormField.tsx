import { useId, type ReactNode } from 'react'

export interface FieldControlProps {
  id: string
  status?: 'error'
  'aria-required'?: boolean
  'aria-invalid'?: boolean
  'aria-describedby'?: string
}

interface Props {
  label: string
  required?: boolean
  /** Thông báo lỗi của ô (từ React Hook Form `errors.x?.message`). */
  error?: string
  hint?: ReactNode
  /** Nhận các thuộc tính a11y để gắn vào input antd: `<Input {...field} {...control} />`. */
  children: (control: FieldControlProps) => ReactNode
}

/**
 * Nhãn + ô nhập + lỗi theo UI 25A: mỗi input có <label>, trường bắt buộc có dấu * và chữ "bắt buộc" cho trình đọc
 * màn hình, lỗi gắn với ô bằng aria-describedby và đọc ngay bằng role="alert".
 */
export default function FormField({ label, required, error, hint, children }: Props) {
  const id = useId()
  const errorId = `${id}-error`
  const hintId = `${id}-hint`
  const describedBy = [error ? errorId : null, hint ? hintId : null].filter(Boolean).join(' ') || undefined

  return (
    <div style={{ marginBottom: 16 }}>
      <label htmlFor={id} style={{ display: 'block', marginBottom: 4, fontWeight: 500 }}>
        {label}
        {required && (
          <>
            {' '}
            <span aria-hidden="true">*</span>
            <span className="sr-only"> (bắt buộc)</span>
          </>
        )}
      </label>
      {children({
        id,
        status: error ? 'error' : undefined,
        'aria-required': required || undefined,
        'aria-invalid': !!error,
        'aria-describedby': describedBy,
      })}
      {hint && (
        <div id={hintId} style={{ color: '#6B7280', marginTop: 4 }}>
          {hint}
        </div>
      )}
      {error && (
        <div id={errorId} role="alert" style={{ color: '#B91C1C', marginTop: 4 }}>
          {error}
        </div>
      )}
    </div>
  )
}
