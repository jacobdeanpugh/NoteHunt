const API_BASE_URL = '/api'

async function fetchJSON(url, options = {}) {
  const response = await fetch(url, {
    headers: {
      'Content-Type': 'application/json',
      ...options.headers,
    },
    ...options,
  })

  if (!response.ok) {
    throw new Error(`HTTP error! status: ${response.status}`)
  }

  return response.json()
}

export async function searchNotes(query, page = 1, limit = 20) {
  const offset = (page - 1) * limit
  const params = new URLSearchParams({
    q: query,
    offset,
    limit
  })
  const url = `${API_BASE_URL}/search?${params.toString()}`

  return fetchJSON(url)
}

export async function getIndexStatus() {
  return fetchJSON(`${API_BASE_URL}/index/status`)
}

export async function getConfig() {
  return fetchJSON(`${API_BASE_URL}/config`)
}

export async function getTags() {
  return fetchJSON(`${API_BASE_URL}/tags`)
}
