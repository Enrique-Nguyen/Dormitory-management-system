import { useEffect, useState } from 'react'
import {
  getMaintenancePhotoBlob,
  type MaintenancePhoto,
} from '../../services/api/maintenance'

/**
 * Ảnh hiện trường dùng endpoint kiểm tra quyền. Trình duyệt không gắn Bearer JWT khi tải
 * `<img src>` trực tiếp, nên mỗi ảnh được tải bằng axios rồi hiển thị qua blob URL.
 */
function PhotoThumb({ photo, index }: { photo: MaintenancePhoto; index: number }) {
  const [src, setSrc] = useState<string>()
  const [failed, setFailed] = useState(false)

  useEffect(() => {
    let active = true
    let objectUrl: string | undefined
    getMaintenancePhotoBlob(photo.url)
      .then((blob) => {
        if (!active) return
        objectUrl = URL.createObjectURL(blob)
        setSrc(objectUrl)
      })
      .catch(() => {
        if (active) setFailed(true)
      })
    return () => {
      active = false
      if (objectUrl) URL.revokeObjectURL(objectUrl)
    }
  }, [photo.url])

  const frameStyle = {
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    minHeight: 120,
    padding: 'var(--space-2)',
    border: '1px solid var(--color-border)',
    borderRadius: 'var(--radius-md)',
    background: 'var(--color-bg-alt)',
    color: 'var(--color-text-muted)',
    fontSize: 13,
    textAlign: 'center' as const,
  }

  if (failed) return <div style={frameStyle}>Không tải được ảnh {index + 1}</div>
  if (!src) return <div style={frameStyle}>Đang tải ảnh {index + 1}…</div>

  return (
    <a
      href={src}
      target="_blank"
      rel="noreferrer"
      aria-label={`Mở ảnh hiện trường ${index + 1} trong tab mới`}
      style={{ display: 'block' }}
    >
      <img
        src={src}
        alt={`Ảnh hiện trường ${index + 1}`}
        loading="lazy"
        style={{
          display: 'block',
          width: '100%',
          aspectRatio: '4 / 3',
          objectFit: 'cover',
          borderRadius: 'var(--radius-md)',
          border: '1px solid var(--color-border)',
          background: 'var(--color-bg-alt)',
        }}
      />
    </a>
  )
}

export default function MaintenancePhotoGallery({
  photos,
}: {
  photos: MaintenancePhoto[]
}) {
  if (photos.length === 0)
    return (
      <p className="dms-page-subtitle" style={{ margin: 0 }}>
        Chưa có ảnh đính kèm.
      </p>
    )

  return (
    <div
      style={{
        display: 'grid',
        gridTemplateColumns: 'repeat(auto-fill, minmax(140px, 1fr))',
        gap: 'var(--space-3)',
      }}
    >
      {photos.map((photo, index) => (
        <PhotoThumb key={photo.id} photo={photo} index={index} />
      ))}
    </div>
  )
}
