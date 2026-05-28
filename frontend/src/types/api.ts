export type Doctor = {
  id: number;
  fullName: string;
  specialty: string;
  clinicName: string;
  bio: string;
  addressLine: string;
  city: string;
  state: string;
  postalCode: string;
  latitude: number | null;
  longitude: number | null;
  phone: string | null;
  rating: number | null;
  yearsExperience: number | null;
  acceptingNewPatients: boolean;
  telehealth: boolean;
  nextAvailable: string | null;
  externalProvider: string | null;
  externalId: string | null;
  externalUri: string | null;
  languages: string[];
  acceptedInsurances: string[];
  profileTags: string[];
};

export type RecommendationPayload = {
  symptoms: string;
  latitude?: number;
  longitude?: number;
  radiusMiles?: number;
  insurance?: string;
  language?: string;
  telehealthPreferred?: boolean;
};

export type RecommendationResult = {
  rank: number;
  matchScore: number;
  reason: string;
  distanceMiles?: number;
  placesSource: string;
  placeName: string;
  placeAddress: string;
  doctor: Doctor;
};

export type RecommendationResponse = {
  requestId: number;
  requestedAt: string;
  cacheHit: boolean;
  extractionSource: string;
  extractedTerms: string[];
  predictedSpecialties: string[];
  results: RecommendationResult[];
  disclaimer: string;
};

export type Specialty = {
  id: number;
  name: string;
  description: string;
};

export type LocationSearchResult = {
  label: string;
  address: string | null;
  latitude: number;
  longitude: number;
  source: string;
};

export type CreateDoctorPayload = {
  fullName: string;
  specialty: string;
  clinicName: string;
  bio: string;
  addressLine: string;
  city: string;
  state: string;
  postalCode: string;
  latitude: number;
  longitude: number;
  phone: string;
  rating: number;
  yearsExperience: number;
  acceptingNewPatients: boolean;
  telehealth: boolean;
  nextAvailable: string;
  externalProvider?: string;
  externalId?: string;
  externalUri?: string;
  languages: string[];
  acceptedInsurances: string[];
  profileTags: string[];
};

export type FeedbackPayload = {
  recommendationRequestId: number;
  doctorId: number;
  helpfulRating: number;
  wouldContactProvider: boolean;
  comment?: string;
};

export type FeedbackResponse = FeedbackPayload & {
  id: number;
  createdAt: string;
};
