import { useInfiniteQuery, useQuery } from '@tanstack/react-query'
import { useEffect, useState } from 'react'
import { Select } from '../../components/ui/FormControls'
import { api } from '../../services/api/client'
import { useMe } from '../account/useMe'
import { findReferenceRecord, readValue, searchByPage } from './api'
import type { CrudRecord } from './types'
import type { FieldControlProps } from '../../components/ui/FormField'

export function ReferenceLabel({ endpoint, value, valueField = 'code' }: { endpoint: string; value: unknown; valueField?: string }) {
  const { data: me } = useMe()
  const query = useQuery({
    queryKey: ['crud-lookup', me?.id, endpoint, 'label', valueField, value],
    enabled: value != null && value !== '',
    staleTime: 60_000,
    queryFn: async () => {
      return (await findReferenceRecord(endpoint, valueField, String(value)))?.name
    },
  })
  return <span>{String(query.data ?? value ?? '—')}</span>
}

type ReferenceValue = string | number
interface ReferenceOption {
  value: ReferenceValue
  label: string
  disabled?: boolean
}
interface ReferenceSelectProps extends Partial<FieldControlProps> {
  endpoint: string
  filters?: Record<string, string | number | boolean>
  valueField?: string
  labelField?: string
  value?: ReferenceValue | ReferenceValue[]
  onChange?: (value: ReferenceValue | ReferenceValue[] | undefined) => void
  initialOptions?: ReferenceOption[]
  multiple?: boolean
  disabled?: boolean
  allowClear?: boolean
  placeholder?: string
  'aria-label'?: string
}

export default function ReferenceSelect({
  endpoint, filters = {}, valueField = 'id', labelField = 'name', value, onChange,
  initialOptions = [], multiple, disabled, allowClear, placeholder = 'Chọn…',
  'aria-label': ariaLabel, ...controlProps
}: ReferenceSelectProps) {
  const { data: me } = useMe()
  const [search, setSearch] = useState('')
  const [keyword, setKeyword] = useState('')
  useEffect(() => {
    const timer = setTimeout(() => setKeyword(search.trim()), 300)
    return () => clearTimeout(timer)
  }, [search])
  const query = useInfiniteQuery({
    queryKey: ['crud-lookup', me?.id, endpoint, filters, keyword],
    initialPageParam: 0,
    queryFn: ({ pageParam }) => searchByPage(endpoint, { ...filters, q: keyword, page: pageParam, size: 50 }),
    getNextPageParam: (last, pages) => pages.reduce((count, page) => count + page.content.length, 0) < last.totalElements && last.content.length ? pages.length : undefined,
    enabled: !disabled,
    staleTime: 60_000,
  })
  const selectedValues = Array.isArray(value) ? value : value == null ? [] : [value]
  const selected = useQuery({
    queryKey: ['crud-lookup', me?.id, endpoint, 'selected', filters, valueField, selectedValues],
    enabled: !disabled && selectedValues.length > 0,
    staleTime: 60_000,
    queryFn: async () => {
      const selectionFilters = Object.fromEntries(Object.entries(filters).filter(([key]) => key !== 'active'))
      const rows = await Promise.all(selectedValues.map(async (item) => {
        if (valueField === 'id') return (await api.get(`${endpoint}/${encodeURIComponent(String(item))}`)).data as CrudRecord
        return findReferenceRecord(endpoint, valueField, item, selectionFilters)
      }))
      return rows.filter((row): row is CrudRecord => !!row)
    },
  })
  const options = new Map<ReferenceValue, ReferenceOption>()
  selectedValues.forEach((item) => options.set(item, { value: item, label: String(item) }))
  initialOptions.forEach((option) => options.set(option.value, option))
  const rows = [...(selected.data ?? []), ...(query.data?.pages.flatMap((page) => page.content) ?? [])]
  rows.forEach((row) => {
    const item = readValue(row, valueField)
    if (typeof item !== 'string' && typeof item !== 'number') return
    const label = readValue(row, labelField)
    const context = row.parentName ?? row.schoolName
    options.set(item, { value: item, label: [label ?? item, context].filter((part) => part != null && part !== '').join(' · '), disabled: filters.active === true && row.active === false })
  })
  return (
    <Select
      {...controlProps}
      aria-label={ariaLabel}
      value={value}
      onChange={onChange}
      showSearch
      filterOption={false}
      onSearch={setSearch}
      onOpenChange={(open) => { if (open && query.isError) void query.refetch() }}
      onPopupScroll={(event) => {
        const target = event.currentTarget
        if (target.scrollHeight - target.scrollTop - target.clientHeight < 40 && query.hasNextPage && !query.isFetchingNextPage) void query.fetchNextPage()
      }}
      mode={multiple ? 'multiple' : undefined}
      disabled={disabled}
      allowClear={allowClear}
      loading={query.isFetching}
      options={[...options.values()]}
      placeholder={query.isError ? 'Không tải được danh sách, hãy mở lại để thử' : placeholder}
      notFoundContent={query.isError ? 'Không tải được danh sách' : undefined}
    />
  )
}
