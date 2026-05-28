import type {
  CreateDoctorPayload,
  Doctor,
  FeedbackPayload,
  FeedbackResponse,
  LocationSearchResult,
  RecommendationPayload,
  RecommendationResponse,
  Specialty
} from '../types/api';

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080';

type ApiOptions = {
  token?: string | null;
};

async function request<T>(path: string, init: RequestInit = {}, options: ApiOptions = {}): Promise<T> {
  const headers = new Headers(init.headers);
  headers.set('Content-Type', 'application/json');
  if (options.token) {
    headers.set('Authorization', `Bearer ${options.token}`);
  }

  const response = await fetch(`${API_BASE_URL}${path}`, {
    ...init,
    headers
  });

  if (!response.ok) {
    const message = await response.text();
    throw new Error(message || `Request failed with status ${response.status}`);
  }

  return response.json() as Promise<T>;
}

export function getDoctors(token?: string | null) {
  return request<Doctor[]>('/api/doctors', {}, { token });
}

export function getSpecialties(token?: string | null) {
  return request<Specialty[]>('/api/specialties', {}, { token });
}

export function searchLocations(query: string, token?: string | null) {
  return request<LocationSearchResult[]>(`/api/locations/search?query=${encodeURIComponent(query)}`, {}, { token });
}

export function recommend(payload: RecommendationPayload, token?: string | null) {
  return request<RecommendationResponse>(
    '/api/recommendations',
    {
      method: 'POST',
      body: JSON.stringify(payload)
    },
    { token }
  );
}

export function createDoctor(payload: CreateDoctorPayload, token?: string | null) {
  return request<Doctor>(
    '/api/admin/doctors',
    {
      method: 'POST',
      body: JSON.stringify(payload)
    },
    { token }
  );
}

export function submitFeedback(payload: FeedbackPayload, token?: string | null) {
  return request<FeedbackResponse>(
    '/api/feedback',
    {
      method: 'POST',
      body: JSON.stringify(payload)
    },
    { token }
  );
}
