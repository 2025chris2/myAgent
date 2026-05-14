// 共享的下载链接匹配正则（与 MessageBubble.vue 保持一致）
export const DOWNLOAD_URL_REGEX =
  /(\/api\/files\/download\?file=([^\s<>，。!！?\n)]+))|([^\s]*\/tmp\/([^\s,，。!！?\n]+))/g

// 外部图片链接正则（Pexels 等，无 capture group，用 match[0] 取值）
export const IMAGE_URL_REGEX =
  /https?:\/\/[^\s<>，。!！?\n)]+\.(?:jpg|jpeg|png|webp|gif|bmp|svg)(?:\?[^\s<>，。!！?\n)]*)?/gi

// 从文本中提取所有唯一的下载 URL（含外部图片 URL）
export function extractDownloadUrls(text) {
  if (!text) return []
  const urls = new Set()
  const downloadRegex = new RegExp(DOWNLOAD_URL_REGEX.source, DOWNLOAD_URL_REGEX.flags)
  let match
  while ((match = downloadRegex.exec(text)) !== null) {
    if (match[2]) {
      urls.add(match[1])
    } else if (match[3]) {
      const fullPath = match[3]
      const relativePath = fullPath.substring(fullPath.indexOf('/tmp/') + 5)
      urls.add('/api/files/download?file=' + encodeURIComponent(relativePath))
    }
  }
  const imageRegex = new RegExp(IMAGE_URL_REGEX.source, IMAGE_URL_REGEX.flags)
  while ((match = imageRegex.exec(text)) !== null) {
    urls.add(match[0])
  }
  return [...urls]
}

// 获取 blob 并触发浏览器下载
export async function downloadFile(downloadUrl) {
  // 外部图片 URL：直接 fetch 后下载
  if (/^https?:\/\//.test(downloadUrl)) {
    const resp = await fetch(downloadUrl)
    if (!resp.ok) throw new Error(`Download failed: ${resp.status}`)
    const blob = await resp.blob()
    const fileName = downloadUrl.split('/').pop().split('?')[0] || 'image'
    const url = window.URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = fileName
    document.body.appendChild(a)
    a.click()
    document.body.removeChild(a)
    window.URL.revokeObjectURL(url)
    return
  }

  const resp = await fetch(downloadUrl, {
    headers: { 'X-Auto-Download': 'true' },
  })
  if (!resp.ok) throw new Error(`Download failed: ${resp.status}`)

  const blob = await resp.blob()

  // 从 Content-Disposition 头提取文件名
  let fileName = 'download'
  const disposition = resp.headers.get('Content-Disposition')
  if (disposition) {
    const utf8Match = disposition.match(/filename\*=UTF-8''([^;]+)/)
    if (utf8Match) {
      fileName = decodeURIComponent(utf8Match[1])
    } else {
      const plainMatch = disposition.match(/filename="([^"]+)"/)
      if (plainMatch) fileName = plainMatch[1]
    }
  }

  const url = window.URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = fileName
  document.body.appendChild(a)
  a.click()
  document.body.removeChild(a)
  window.URL.revokeObjectURL(url)
}
