const API_BASE_URL = import.meta.env.VITE_API_URL || 'http://localhost:8083';

export class ApiError extends Error {
  constructor(status, message, data) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
    this.data = data;
  }
}

async function request(endpoint, options = {}) {
  const url = `${API_BASE_URL}${endpoint}`;
  const token = localStorage.getItem('chargeconnect_token');

  const headers = {
    'Content-Type': 'application/json',
    ...options.headers,
  };

  if (token) {
    headers['Authorization'] = `Bearer ${token}`;
  }

  const response = await fetch(url, {
    ...options,
    headers,
  });

  if (response.status === 204) {
    return {};
  }

  let data = null;
  const contentType = response.headers.get('content-type');
  if (contentType && contentType.includes('application/json')) {
    try {
      data = await response.json();
    } catch {
      data = null;
    }
  } else {
    data = await response.text();
  }

  if (!response.ok) {
    let errorMessage = 'An unexpected error occurred';
    if (data && typeof data === 'object') {
      if (data.fieldErrors && Object.keys(data.fieldErrors).length > 0) {
        const fields = Object.entries(data.fieldErrors)
          .map(([k, v]) => `${k}: ${v}`)
          .join(', ');
        errorMessage = `Validation failed: ${fields}`;
      } else if (data.message) {
        errorMessage = data.message;
      } else if (data.error) {
        errorMessage = data.error;
      }
    } else if (typeof data === 'string' && data.length > 0) {
      errorMessage = data;
    }

    if (response.status === 401 && token && !endpoint.includes('/api/auth/login')) {
      localStorage.removeItem('chargeconnect_token');
      localStorage.removeItem('chargeconnect_user');
      window.location.href = '/login';
    }

    throw new ApiError(response.status, errorMessage, data);
  }

  return data;
}

export const api = {
  get: (endpoint, options) => request(endpoint, { ...options, method: 'GET' }),

  post: (endpoint, body, options) =>
    request(endpoint, {
      ...options,
      method: 'POST',
      body: body !== undefined ? JSON.stringify(body) : undefined,
    }),

  patch: (endpoint, body, options) =>
    request(endpoint, {
      ...options,
      method: 'PATCH',
      body: body !== undefined ? JSON.stringify(body) : undefined,
    }),

  delete: (endpoint, options) => request(endpoint, { ...options, method: 'DELETE' }),
};
