import { Dropdown, Tooltip, type MenuProps } from 'antd'
import { useRef, useState, type CSSProperties } from 'react'
import {
  DEFAULT_CUSTOM_ACCENT,
  THEME_STYLES,
  THEME_UI_LABELS,
  V5_ACCENT_PRESETS,
  type ThemeStyle,
} from '../../app/theme'
import { useAppTheme } from '../../app/themeStore'

function PaletteIcon({ className, style }: { className?: string; style?: CSSProperties }) {
  return (
    <svg
      xmlns="http://www.w3.org/2000/svg"
      width="16"
      height="16"
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth="2"
      strokeLinecap="round"
      strokeLinejoin="round"
      className={className}
      style={style}
    >
      <circle cx="13.5" cy="6.5" r=".5" fill="currentColor" />
      <circle cx="17.5" cy="10.5" r=".5" fill="currentColor" />
      <circle cx="8.5" cy="7.5" r=".5" fill="currentColor" />
      <circle cx="6.5" cy="12.5" r=".5" fill="currentColor" />
      <path d="M12 2C6.5 2 2 6.5 2 12s4.5 10 10 10c.926 0 1.648-.746 1.648-1.688 0-.437-.18-.835-.437-1.125-.29-.289-.438-.652-.438-1.125a1.64 1.64 0 0 1 1.668-1.668h1.996c3.051 0 5.555-2.503 5.555-5.554C21.965 6.012 17.461 2 12 2z" />
    </svg>
  )
}

function CheckIcon({ className, style }: { className?: string; style?: CSSProperties }) {
  return (
    <svg
      xmlns="http://www.w3.org/2000/svg"
      width="16"
      height="16"
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth="2"
      strokeLinecap="round"
      strokeLinejoin="round"
      className={className}
      style={style}
    >
      <path d="M20 6 9 17l-5-5" />
    </svg>
  )
}

function PipetteIcon({ className, style }: { className?: string; style?: CSSProperties }) {
  return (
    <svg
      xmlns="http://www.w3.org/2000/svg"
      width="14"
      height="14"
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth="2"
      strokeLinecap="round"
      strokeLinejoin="round"
      className={className}
      style={style}
    >
      <path d="m2 22 1-1h3l9-9" />
      <path d="M3 21v-3l9-9" />
      <path d="m15 6 3.4-3.4a2.1 2.1 0 1 1 3 3L18 9l.4.4a2.1 2.1 0 1 1-3 3l-3.8-3.8a2.1 2.1 0 1 1 3-3l.4.4Z" />
    </svg>
  )
}

function MoonIcon({ className, style }: { className?: string; style?: CSSProperties }) {
  return (
    <svg
      xmlns="http://www.w3.org/2000/svg"
      width="16"
      height="16"
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth="2"
      strokeLinecap="round"
      strokeLinejoin="round"
      className={className}
      style={style}
    >
      <path d="M12 3a6 6 0 0 0 9 9 9 9 0 1 1-9-9Z" />
    </svg>
  )
}

function SunIcon({ className, style }: { className?: string; style?: CSSProperties }) {
  return (
    <svg
      xmlns="http://www.w3.org/2000/svg"
      width="16"
      height="16"
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth="2"
      strokeLinecap="round"
      strokeLinejoin="round"
      className={className}
      style={style}
    >
      <circle cx="12" cy="12" r="4" />
      <path d="M12 2v2" />
      <path d="M12 20v2" />
      <path d="m4.93 4.93 1.41 1.41" />
      <path d="m17.66 17.66 1.41 1.41" />
      <path d="M2 12h2" />
      <path d="M20 12h2" />
      <path d="m6.34 17.66-1.41 1.41" />
      <path d="m19.07 4.93-1.41 1.41" />
    </svg>
  )
}

function Grid3x3Icon({ className, style }: { className?: string; style?: CSSProperties }) {
  return (
    <svg
      xmlns="http://www.w3.org/2000/svg"
      width="16"
      height="16"
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth="2"
      strokeLinecap="round"
      strokeLinejoin="round"
      className={className}
      style={style}
    >
      <rect width="18" height="18" x="3" y="3" rx="2" />
      <path d="M3 9h18" />
      <path d="M3 15h18" />
      <path d="M9 3v18" />
      <path d="M15 3v18" />
    </svg>
  )
}

function SwatchSquare({ color, bg }: { color: string; bg: string }) {
  return (
    <span className="dms-swatch-square" aria-hidden>
      <span style={{ backgroundColor: color }} />
      <span style={{ backgroundColor: bg }} />
    </span>
  )
}

export default function ThemeSwitcher() {
  const theme = useAppTheme()
  const custom = theme.customAccent ?? DEFAULT_CUSTOM_ACCENT
  const selectedKey: string =
    theme.customAccent?.key ??
    (theme.accent === 'amber'
      ? 'orange'
      : theme.accent === 'emerald'
        ? 'green'
        : theme.accent === 'violet'
          ? 'purple'
          : theme.accent)
  const colorInputRef = useRef<HTMLInputElement>(null)
  const colorBgInputRef = useRef<HTMLInputElement>(null)
  const [open, setOpen] = useState(false)
  const keepOpenRef = useRef(false)

  const items: MenuProps['items'] = [
    {
      type: 'group',
      label: THEME_UI_LABELS.appearance,
      children: (
        Object.entries(THEME_STYLES) as [
          ThemeStyle,
          (typeof THEME_STYLES)[ThemeStyle],
        ][]
      ).map(([key, preset]) => ({
        key: `style-${key}`,
        label: (
          <span className="dms-theme-option">
            <span className="dms-theme-style-description">
              <span style={{ fontWeight: 500 }}>{preset.label}</span>
              <small>{preset.description}</small>
            </span>
            {theme.style === key && <CheckIcon className="dms-theme-check" />}
          </span>
        ),
        onClick: () => {
          theme.setStyle(key)
          setOpen(false)
        },
      })),
    },
    { type: 'divider' },
    {
      type: 'group',
      label: THEME_UI_LABELS.accent,
      children: [
        ...V5_ACCENT_PRESETS.map((preset) => {
          const isSelected = selectedKey === preset.key
          return {
            key: `accent-${preset.key}`,
            label: (
              <span className="dms-theme-option">
                <SwatchSquare color={preset.swatch} bg={preset.swatchBg} />
                <span style={{ flex: 1 }}>{preset.label}</span>
                {isSelected && <CheckIcon className="dms-theme-check" />}
              </span>
            ),
            onClick: () => {
              if (preset.key === 'default' || preset.key === 'yellow') {
                theme.setCustomAccent({
                  key: preset.key,
                  primary: preset.swatch,
                  light: preset.swatchBg,
                })
              } else if (preset.backendKey) {
                theme.setAccent(preset.backendKey)
              }
              setOpen(false)
            },
          }
        }),
        {
          key: 'accent-custom',
          label: (
            <span className="dms-theme-option">
              <span
                className="dms-swatch-square"
                onClick={(e) => e.stopPropagation()}
              >
                <button
                  type="button"
                  className="dms-swatch-btn"
                  title="Chọn màu chủ đạo"
                  style={{ backgroundColor: custom.primary }}
                  onClick={(e) => {
                    e.stopPropagation()
                    keepOpenRef.current = true
                    setTimeout(() => colorInputRef.current?.click(), 0)
                  }}
                />
                <button
                  type="button"
                  className="dms-swatch-btn"
                  title="Chọn màu nền nhấn"
                  style={{ backgroundColor: custom.light }}
                  onClick={(e) => {
                    e.stopPropagation()
                    keepOpenRef.current = true
                    setTimeout(() => colorBgInputRef.current?.click(), 0)
                  }}
                />
              </span>
              <PipetteIcon
                className="dms-theme-item-icon"
                style={{ width: 14, height: 14, color: 'var(--color-text-muted)' }}
              />
              <span style={{ flex: 1 }}>{THEME_UI_LABELS.custom}</span>
              {selectedKey === 'custom' && <CheckIcon className="dms-theme-check" />}
            </span>
          ),
          onClick: () => {
            theme.setCustomAccent({ ...custom, key: 'custom' })
            setOpen(false)
          },
        },
      ],
    },
    { type: 'divider' },
    {
      key: 'mode-toggle',
      label: (
        <span className="dms-theme-option">
          {theme.mode === 'light' ? (
            <MoonIcon className="dms-theme-item-icon" />
          ) : (
            <SunIcon className="dms-theme-item-icon" />
          )}
          <span>
            {theme.mode === 'light'
              ? THEME_UI_LABELS.darkMode
              : THEME_UI_LABELS.lightMode}
          </span>
        </span>
      ),
      onClick: () => {
        theme.toggleMode()
        setOpen(false)
      },
    },
    {
      key: 'table-borders',
      label: (
        <span className="dms-theme-option">
          <Grid3x3Icon className="dms-theme-item-icon" />
          <span style={{ flex: 1 }}>{THEME_UI_LABELS.tableBorders}</span>
          <span className="dms-theme-status-text">
            {theme.tableBorders ? THEME_UI_LABELS.yes : THEME_UI_LABELS.no}
          </span>
        </span>
      ),
      onClick: (info) => {
        info?.domEvent?.stopPropagation?.()
        keepOpenRef.current = true
        theme.setTableBorders(!theme.tableBorders)
      },
    },
  ]

  return (
    <div style={{ display: 'inline-flex', alignItems: 'center' }}>
      {/* Hidden color inputs — đặt ngoài Dropdown để không bị unmount khi menu đóng */}
      <input
        ref={colorInputRef}
        type="color"
        className="dms-sr-only"
        value={custom.primary}
        onChange={(e) =>
          theme.setCustomAccent({
            key: 'custom',
            primary: e.target.value,
            light: custom.light,
          })
        }
      />
      <input
        ref={colorBgInputRef}
        type="color"
        className="dms-sr-only"
        value={custom.light}
        onChange={(e) =>
          theme.setCustomAccent({
            key: 'custom',
            primary: custom.primary,
            light: e.target.value,
          })
        }
      />
      <Tooltip title={open ? '' : THEME_UI_LABELS.title}>
        <Dropdown
          open={open}
          onOpenChange={(nextOpen, info) => {
            if (info?.source === 'menu' && keepOpenRef.current) {
              keepOpenRef.current = false
              return
            }
            setOpen(nextOpen)
          }}
          menu={{ items, className: 'dms-theme-menu' }}
          trigger={['click']}
          placement="bottomRight"
        >
          <button
            type="button"
            className="dms-theme-trigger-btn"
            aria-label={THEME_UI_LABELS.title}
          >
            <PaletteIcon className="dms-theme-trigger-icon" />
          </button>
        </Dropdown>
      </Tooltip>
    </div>
  )
}
