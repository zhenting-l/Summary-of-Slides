import fs from 'node:fs/promises'
import path from 'node:path'

const OWNER = 'zhenting-l'
const REPO = 'dailyarxiv_sync'
const TAG = 'latest-apk'

function getToken() {
  const token = process.env.DAILYARXIV_GITHUB_TOKEN || process.env.GITHUB_TOKEN || process.env.GH_TOKEN
  if (!token) {
    throw new Error('Missing GitHub token in env DAILYARXIV_GITHUB_TOKEN / GITHUB_TOKEN / GH_TOKEN')
  }
  return token
}

async function apiJson(method, url, { token, body } = {}) {
  const res = await fetch(url, {
    method,
    headers: {
      Accept: 'application/vnd.github+json',
      Authorization: `Bearer ${token}`,
      'X-GitHub-Api-Version': '2022-11-28',
    },
    body: body ? JSON.stringify(body) : undefined,
  })

  if (!res.ok) {
    const text = await res.text().catch(() => '')
    throw new Error(`GitHub API ${method} ${url} failed: ${res.status} ${res.statusText}${text ? `\n${text}` : ''}`)
  }

  return res.json()
}

async function apiNoBody(method, url, { token } = {}) {
  const res = await fetch(url, {
    method,
    headers: {
      Accept: 'application/vnd.github+json',
      Authorization: `Bearer ${token}`,
      'X-GitHub-Api-Version': '2022-11-28',
    },
  })

  if (!res.ok) {
    const text = await res.text().catch(() => '')
    throw new Error(`GitHub API ${method} ${url} failed: ${res.status} ${res.statusText}${text ? `\n${text}` : ''}`)
  }
}

async function uploadAsset({ token, uploadUrlTemplate, filePath, name, contentType }) {
  const data = await fs.readFile(filePath)
  const uploadUrl = uploadUrlTemplate.replace('{?name,label}', `?name=${encodeURIComponent(name)}`)

  const res = await fetch(uploadUrl, {
    method: 'POST',
    headers: {
      Accept: 'application/vnd.github+json',
      Authorization: `Bearer ${token}`,
      'X-GitHub-Api-Version': '2022-11-28',
      'Content-Type': contentType,
      'Content-Length': String(data.byteLength),
    },
    body: data,
  })

  if (!res.ok) {
    const text = await res.text().catch(() => '')
    throw new Error(`Upload ${name} failed: ${res.status} ${res.statusText}${text ? `\n${text}` : ''}`)
  }
}

async function main() {
  const token = getToken()

  const release = await apiJson('GET', `https://api.github.com/repos/${OWNER}/${REPO}/releases/tags/${TAG}`, { token })
  const uploadUrlTemplate = release.upload_url

  const existingAssets = Array.isArray(release.assets) ? release.assets : []
  const namesToReplace = new Set(['app-debug.apk', 'app-release.apk'])

  for (const asset of existingAssets) {
    if (namesToReplace.has(asset.name)) {
      await apiNoBody('DELETE', `https://api.github.com/repos/${OWNER}/${REPO}/releases/assets/${asset.id}`, { token })
    }
  }

  const repoRoot = process.cwd()
  const debugApk = path.join(repoRoot, 'app', 'build', 'outputs', 'apk', 'debug', 'app-debug.apk')
  const releaseApk = path.join(repoRoot, 'app', 'build', 'outputs', 'apk', 'release', 'app-release.apk')

  await uploadAsset({
    token,
    uploadUrlTemplate,
    filePath: debugApk,
    name: 'app-debug.apk',
    contentType: 'application/vnd.android.package-archive',
  })

  await uploadAsset({
    token,
    uploadUrlTemplate,
    filePath: releaseApk,
    name: 'app-release.apk',
    contentType: 'application/vnd.android.package-archive',
  })

  console.log(`Uploaded APKs to release tag ${TAG}`)
}

await main()
