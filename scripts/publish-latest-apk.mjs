import fs from 'node:fs/promises'
import path from 'node:path'

const OWNER = 'zhenting-l'
const REPO = 'Summary-of-Slides'

function getToken() {
  const token = process.env.GITHUB_TOKEN || process.env.GH_TOKEN
  if (!token) {
    throw new Error('Missing GitHub token in env GITHUB_TOKEN / GH_TOKEN')
  }
  return token
}

async function resolveReleaseApk(repoRoot) {
  const releaseDir = path.join(repoRoot, 'app', 'build', 'outputs', 'apk', 'release')
  const dirEntries = await fs.readdir(releaseDir, { withFileTypes: true }).catch(() => null)
  if (!dirEntries) {
    throw new Error(`Release APK directory not found: ${releaseDir}`)
  }

  const apkFiles = dirEntries
    .filter(entry => entry.isFile() && entry.name.toLowerCase().endsWith('.apk'))
    .map(entry => path.join(releaseDir, entry.name))

  if (apkFiles.length === 0) {
    const names = dirEntries.map(entry => entry.name).join(', ')
    throw new Error(`No APK found under ${releaseDir}. Existing files: ${names || '(empty)'}`)
  }

  const signedApk =
    apkFiles.find(file => path.basename(file) === 'app-release.apk') ||
    apkFiles.find(file => !path.basename(file).toLowerCase().includes('unsigned'))

  if (!signedApk) {
    const fileNames = apkFiles.map(file => path.basename(file)).join(', ')
    throw new Error(`Only unsigned APKs were found under ${releaseDir}: ${fileNames}`)
  }

  return signedApk
}

async function getVersionName(repoRoot) {
  const gradleProperties = await fs.readFile(path.join(repoRoot, 'gradle.properties'), 'utf8')
  const match = gradleProperties.match(/^APP_VERSION_NAME=(.+)$/m)
  if (!match) {
    throw new Error('APP_VERSION_NAME not found in gradle.properties')
  }
  return match[1].trim()
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
  const repoRoot = process.cwd()
  const versionName = await getVersionName(repoRoot)
  const tag = `v${versionName}`
  const releaseApkName = `Summary-of-Slides-v${versionName}.apk`
  const releaseApk = await resolveReleaseApk(repoRoot)

  let release
  try {
    release = await apiJson('GET', `https://api.github.com/repos/${OWNER}/${REPO}/releases/tags/${tag}`, { token })
  } catch (error) {
    const message = String(error?.message || error)
    if (!message.includes('404')) throw error
    release = await apiJson('POST', `https://api.github.com/repos/${OWNER}/${REPO}/releases`, {
      token,
      body: {
        tag_name: tag,
        name: tag,
        draft: false,
        prerelease: false,
        generate_release_notes: true,
      },
    })
  }
  const uploadUrlTemplate = release.upload_url

  const existingAssets = Array.isArray(release.assets) ? release.assets : []
  const namesToReplace = new Set(['app-release.apk', releaseApkName])

  for (const asset of existingAssets) {
    if (namesToReplace.has(asset.name)) {
      await apiNoBody('DELETE', `https://api.github.com/repos/${OWNER}/${REPO}/releases/assets/${asset.id}`, { token })
    }
  }

  await uploadAsset({
    token,
    uploadUrlTemplate,
    filePath: releaseApk,
    name: releaseApkName,
    contentType: 'application/vnd.android.package-archive',
  })

  console.log(`Uploaded ${releaseApkName} from ${path.basename(releaseApk)} to release tag ${tag}`)
}

await main()
