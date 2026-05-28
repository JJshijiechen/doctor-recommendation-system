import { GoogleLogin, type CredentialResponse } from '@react-oauth/google';
import {
  Activity,
  BadgePlus,
  CheckCircle2,
  ClipboardList,
  Clock3,
  ExternalLink,
  HeartPulse,
  Languages,
  LocateFixed,
  MapPin,
  PhoneCall,
  Search,
  ShieldCheck,
  SlidersHorizontal,
  Sparkles,
  Stethoscope,
  UserRound
} from 'lucide-react';
import { FormEvent, KeyboardEvent, useEffect, useMemo, useState } from 'react';
import { createDoctor, getDoctors, getSpecialties, recommend, searchLocations, submitFeedback } from './lib/api';
import type { CreateDoctorPayload, Doctor, LocationSearchResult, RecommendationPayload, RecommendationResponse, Specialty } from './types/api';

type AppProps = {
  googleOAuthEnabled: boolean;
};

type View = 'match' | 'directory' | 'admin';

const initialPayload: RecommendationPayload = {
  symptoms: 'chest pain and shortness of breath',
  latitude: 41.8781,
  longitude: -87.6298,
  radiusMiles: 20,
  insurance: 'Aetna',
  language: 'English',
  telehealthPreferred: true
};

const initialDoctor: CreateDoctorPayload = {
  fullName: '',
  specialty: 'Family Medicine',
  clinicName: '',
  bio: '',
  addressLine: '',
  city: 'Chicago',
  state: 'IL',
  postalCode: '',
  latitude: 41.8781,
  longitude: -87.6298,
  phone: '',
  rating: 4.6,
  yearsExperience: 5,
  acceptingNewPatients: true,
  telehealth: true,
  nextAvailable: 'This week',
  languages: ['English'],
  acceptedInsurances: ['Aetna'],
  profileTags: ['primary care']
};

const initialLocationQuery = 'Chicago, IL';

function App({ googleOAuthEnabled }: AppProps) {
  const [activeView, setActiveView] = useState<View>('match');
  const [token, setToken] = useState<string | null>(null);
  const [patientName, setPatientName] = useState('Demo Patient');
  const [payload, setPayload] = useState<RecommendationPayload>(initialPayload);
  const [recommendation, setRecommendation] = useState<RecommendationResponse | null>(null);
  const [doctors, setDoctors] = useState<Doctor[]>([]);
  const [specialties, setSpecialties] = useState<Specialty[]>([]);
  const [selectedDoctor, setSelectedDoctor] = useState<Doctor | null>(null);
  const [adminDoctor, setAdminDoctor] = useState<CreateDoctorPayload>(initialDoctor);
  const [loading, setLoading] = useState(false);
  const [locating, setLocating] = useState(false);
  const [locationStatus, setLocationStatus] = useState<string | null>(null);
  const [locationQuery, setLocationQuery] = useState(initialLocationQuery);
  const [locationSuggestions, setLocationSuggestions] = useState<LocationSearchResult[]>([]);
  const [locationSearching, setLocationSearching] = useState(false);
  const [locationMenuOpen, setLocationMenuOpen] = useState(false);
  const [feedbackRating, setFeedbackRating] = useState(5);
  const [feedbackWouldContact, setFeedbackWouldContact] = useState(true);
  const [feedbackComment, setFeedbackComment] = useState('');
  const [feedbackStatus, setFeedbackStatus] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    void refreshDirectory();
  }, []);

  const selectedResult = recommendation?.results.find((result) => result.doctor.id === selectedDoctor?.id);
  const visibleDoctors = recommendation?.results.map((result) => result.doctor) ?? doctors;

  const specialtyOptions = useMemo(() => {
    const names = specialties.map((specialty) => specialty.name);
    return names.length ? names : ['Family Medicine', 'Cardiology', 'Pulmonology', 'Dermatology'];
  }, [specialties]);

  async function refreshDirectory() {
    try {
      const [doctorData, specialtyData] = await Promise.all([getDoctors(token), getSpecialties(token)]);
      setDoctors(doctorData);
      setSpecialties(specialtyData);
      setSelectedDoctor((current) => current ?? doctorData[0] ?? null);
    } catch (requestError) {
      setError(readError(requestError));
    }
  }

  async function submitRecommendation(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setLoading(true);
    setError(null);
    try {
      const response = await recommend(payload, token);
      setRecommendation(response);
      setSelectedDoctor(response.results[0]?.doctor ?? null);
      setActiveView('match');
    } catch (requestError) {
      setError(readError(requestError));
    } finally {
      setLoading(false);
    }
  }

  async function submitDoctor(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setLoading(true);
    setError(null);
    try {
      const created = await createDoctor(adminDoctor, token);
      setDoctors((current) => [created, ...current]);
      setSelectedDoctor(created);
      setAdminDoctor(initialDoctor);
      setActiveView('directory');
    } catch (requestError) {
      setError(readError(requestError));
    } finally {
      setLoading(false);
    }
  }

  async function submitSelectedFeedback(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!recommendation || !selectedDoctor) {
      return;
    }

    setFeedbackStatus(null);
    setError(null);
    try {
      await submitFeedback(
        {
          recommendationRequestId: recommendation.requestId,
          doctorId: selectedDoctor.id,
          helpfulRating: feedbackRating,
          wouldContactProvider: feedbackWouldContact,
          comment: feedbackComment
        },
        token
      );
      setFeedbackStatus('Feedback saved for validation metrics');
      setFeedbackComment('');
    } catch (requestError) {
      setError(readError(requestError));
    }
  }

  function handleGoogleSuccess(response: CredentialResponse) {
    setToken(response.credential ?? null);
    setPatientName('Google Patient');
  }

  function updatePayload<K extends keyof RecommendationPayload>(key: K, value: RecommendationPayload[K]) {
    setPayload((current) => ({ ...current, [key]: value }));
  }

  function updateAdmin<K extends keyof CreateDoctorPayload>(key: K, value: CreateDoctorPayload[K]) {
    setAdminDoctor((current) => ({ ...current, [key]: value }));
  }

  function selectLocation(option: LocationSearchResult) {
    updatePayload('latitude', roundCoordinate(option.latitude));
    updatePayload('longitude', roundCoordinate(option.longitude));
    setLocationQuery(formatLocation(option));
    setLocationStatus(`${formatLocation(option)} selected via ${sourceLabel(option.source)}`);
    setLocationMenuOpen(false);
  }

  function handleLocationInput(value: string) {
    setLocationQuery(value);
    setLocationStatus(null);
    setLocationSuggestions([]);
    setLocationMenuOpen(true);
  }

  async function handleLocationKeyDown(event: KeyboardEvent<HTMLInputElement>) {
    if (event.key === 'Escape') {
      setLocationMenuOpen(false);
      return;
    }

    if (event.key === 'Enter') {
      event.preventDefault();
      if (locationSuggestions[0]) {
        selectLocation(locationSuggestions[0]);
        return;
      }
      await resolveLocationQuery();
    }
  }

  async function resolveLocationQuery() {
    const query = locationQuery.trim();
    if (!query) {
      setLocationStatus('Enter a city, address, hospital, landmark, or coordinates.');
      setLocationSuggestions([]);
      return;
    }

    setLocationSearching(true);
    setLocationStatus('Searching live maps...');
    setError(null);
    try {
      const results = await searchLocations(query, token);
      setLocationSuggestions(results);
      setLocationMenuOpen(true);
      if (results[0]) {
        selectLocation(results[0]);
        setLocationSuggestions(results);
        setLocationMenuOpen(results.length > 1);
      } else {
        setLocationStatus('No live location match. Try a fuller address or latitude, longitude.');
      }
    } catch (requestError) {
      setLocationStatus('Location search failed. Try current location or enter coordinates manually.');
      setError(readError(requestError));
    } finally {
      setLocationSearching(false);
    }
  }

  function useCurrentLocation() {
    setError(null);
    setLocationStatus(null);

    if (!navigator.geolocation) {
      setLocationStatus('Location is not available in this browser.');
      return;
    }

    setLocating(true);
    navigator.geolocation.getCurrentPosition(
      (position) => {
        updatePayload('latitude', roundCoordinate(position.coords.latitude));
        updatePayload('longitude', roundCoordinate(position.coords.longitude));
        setLocationQuery('Current location');
        setLocationSuggestions([]);
        setLocationStatus('Location updated');
        setLocating(false);
      },
      (geoError) => {
        setLocationStatus(readGeolocationError(geoError));
        setLocating(false);
      },
      {
        enableHighAccuracy: true,
        timeout: 10000,
        maximumAge: 300000
      }
    );
  }

  return (
    <main className="app-shell">
      <aside className="control-rail">
        <div className="brand-mark">
          <div className="brand-icon">
            <HeartPulse size={28} />
          </div>
          <div>
            <p>CareMatch</p>
            <span>Doctor Recommendation System</span>
          </div>
        </div>

        <section className="auth-panel">
          <div className="panel-title">
            <ShieldCheck size={18} />
            <span>Secure session</span>
          </div>
          <div className="patient-chip">
            <UserRound size={18} />
            <span>{patientName}</span>
          </div>
          {googleOAuthEnabled ? (
            <GoogleLogin onSuccess={handleGoogleSuccess} onError={() => setError('Google sign-in failed')} />
          ) : (
            <button className="secondary-button" type="button" onClick={() => setPatientName('Local Demo Patient')}>
              <CheckCircle2 size={16} />
              Demo login
            </button>
          )}
        </section>

        <nav className="view-tabs" aria-label="Workspace views">
          <button className={activeView === 'match' ? 'active' : ''} onClick={() => setActiveView('match')} type="button">
            <Search size={17} />
            Match
          </button>
          <button className={activeView === 'directory' ? 'active' : ''} onClick={() => setActiveView('directory')} type="button">
            <ClipboardList size={17} />
            Doctors
          </button>
          <button className={activeView === 'admin' ? 'active' : ''} onClick={() => setActiveView('admin')} type="button">
            <BadgePlus size={17} />
            Admin
          </button>
        </nav>

        <section className="rail-metric">
          <span>Care terms</span>
          <strong>{recommendation?.extractionSource === 'openai' ? 'enhanced' : 'local ready'}</strong>
        </section>
        <section className="rail-metric">
          <span>Lookup</span>
          <strong>{recommendation?.cacheHit ? 'repeat match' : 'fresh match'}</strong>
        </section>
      </aside>

      <section className="workspace">
        <header className="workspace-header">
          <div>
            <p className="eyebrow">Symptom-guided provider matching</p>
            <h1>Patient-guided doctor lookup</h1>
          </div>
          <button className="icon-button" type="button" onClick={() => void refreshDirectory()} aria-label="Refresh doctors">
            <Activity size={20} />
          </button>
        </header>

        {error && <div className="error-banner">{error}</div>}

        {activeView === 'match' && (
          <div className="match-grid">
            <form className="intake-panel" onSubmit={submitRecommendation}>
              <div className="section-heading">
                <Stethoscope size={19} />
                <span>Symptoms</span>
              </div>
              <textarea
                value={payload.symptoms}
                onChange={(event) => updatePayload('symptoms', event.target.value)}
                required
                maxLength={2000}
                aria-label="Symptoms"
              />

              <div className="location-picker">
                <div className="location-field">
                  <label htmlFor="location-search">Location</label>
                  <div className="location-combobox">
                    <Search className="location-search-icon" size={17} aria-hidden="true" />
                    <input
                      id="location-search"
                      value={locationQuery}
                      onChange={(event) => handleLocationInput(event.target.value)}
                      onFocus={() => setLocationMenuOpen(true)}
                      onBlur={() => window.setTimeout(() => setLocationMenuOpen(false), 120)}
                      onKeyDown={handleLocationKeyDown}
                      role="combobox"
                      aria-autocomplete="list"
                      aria-expanded={locationMenuOpen}
                      aria-controls="location-options"
                      placeholder="City, address, hospital, or 41.8781,-87.6298"
                    />
                    <button
                      className="location-resolve-button"
                      type="button"
                      onMouseDown={(event) => event.preventDefault()}
                      onClick={() => void resolveLocationQuery()}
                      disabled={locationSearching}
                      aria-label="Search location"
                    >
                      <MapPin size={16} />
                    </button>
                    {locationMenuOpen && (
                      <div className="location-menu" id="location-options" role="listbox">
                        {locationSearching ? (
                          <div className="location-menu-empty">Searching live maps...</div>
                        ) : locationSuggestions.length ? (
                          locationSuggestions.map((option) => (
                            <button
                              key={`${option.source}-${option.latitude}-${option.longitude}-${option.label}`}
                              type="button"
                              role="option"
                              aria-selected={formatLocation(option) === locationQuery}
                              onMouseDown={(event) => event.preventDefault()}
                              onClick={() => selectLocation(option)}
                            >
                              <span>{formatLocation(option)}</span>
                              <small>
                                {option.latitude.toFixed(4)}, {option.longitude.toFixed(4)}
                              </small>
                            </button>
                          ))
                        ) : locationQuery.trim() ? (
                          <button
                            className="location-menu-action"
                            type="button"
                            role="option"
                            aria-selected="false"
                            onMouseDown={(event) => event.preventDefault()}
                            onClick={() => void resolveLocationQuery()}
                          >
                            <span>Search live maps for "{locationQuery.trim()}"</span>
                            <small>Google Places or SerpAPI</small>
                          </button>
                        ) : (
                          <div className="location-menu-empty">Type any city, address, hospital, landmark, or coordinates</div>
                        )}
                      </div>
                    )}
                  </div>
                </div>
              </div>

              <div className="location-actions">
                <button className="secondary-button location-button" type="button" onClick={useCurrentLocation} disabled={locating}>
                  <LocateFixed size={17} />
                  {locating ? 'Locating...' : 'Use current location'}
                </button>
                {locationStatus && <span aria-live="polite">{locationStatus}</span>}
              </div>

              <div className="form-grid">
                <label>
                  Latitude
                  <input
                    type="number"
                    step="0.0001"
                    value={payload.latitude ?? ''}
                    onChange={(event) => updatePayload('latitude', Number(event.target.value))}
                  />
                </label>
                <label>
                  Longitude
                  <input
                    type="number"
                    step="0.0001"
                    value={payload.longitude ?? ''}
                    onChange={(event) => updatePayload('longitude', Number(event.target.value))}
                  />
                </label>
                <label>
                  Radius
                  <input
                    type="number"
                    min="1"
                    max="100"
                    value={payload.radiusMiles ?? 20}
                    onChange={(event) => updatePayload('radiusMiles', Number(event.target.value))}
                  />
                </label>
                <label>
                  Insurance
                  <input value={payload.insurance ?? ''} onChange={(event) => updatePayload('insurance', event.target.value)} />
                </label>
                <label>
                  Language
                  <input value={payload.language ?? ''} onChange={(event) => updatePayload('language', event.target.value)} />
                </label>
                <label className="toggle-row">
                  <input
                    type="checkbox"
                    checked={Boolean(payload.telehealthPreferred)}
                    onChange={(event) => updatePayload('telehealthPreferred', event.target.checked)}
                  />
                  Telehealth
                </label>
              </div>

              <button className="primary-button" disabled={loading} type="submit">
                <Sparkles size={18} />
                {loading ? 'Ranking...' : 'Find doctors'}
              </button>
            </form>

            <section className="results-panel">
              <div className="section-heading">
                <SlidersHorizontal size={19} />
                <span>Ranked matches</span>
              </div>

              {recommendation && (
                <div className="extraction-strip">
                  <span>{recommendation.predictedSpecialties.join(' / ')}</span>
                  <strong>{recommendation.extractedTerms.join(', ')}</strong>
                </div>
              )}

              <div className="result-list">
                {(recommendation?.results ?? []).map((result) => (
                  <button
                    key={`${result.rank}-${result.doctor.id}`}
                    className={`doctor-result ${selectedDoctor?.id === result.doctor.id ? 'selected' : ''}`}
                    onClick={() => setSelectedDoctor(result.doctor)}
                    type="button"
                  >
                    <span className="rank">#{result.rank}</span>
                    <span>
                      <strong>{result.doctor.fullName}</strong>
                      <small>
                        {result.doctor.specialty} - {sourceLabel(result.doctor.externalProvider ?? result.placesSource)}
                      </small>
                    </span>
                    <span className="score">{Math.round(result.matchScore * 100)}%</span>
                  </button>
                ))}
              </div>

              {!recommendation && (
                <div className="empty-state">
                  <LocateFixed size={38} />
                  <span>Ready for symptom matching</span>
                </div>
              )}
            </section>
          </div>
        )}

        {activeView === 'directory' && (
          <section className="directory-grid">
            {visibleDoctors.map((doctor) => (
              <button
                key={doctor.id}
                className={`doctor-card ${selectedDoctor?.id === doctor.id ? 'selected' : ''}`}
                onClick={() => setSelectedDoctor(doctor)}
                type="button"
              >
                <span className="specialty-pill">{doctor.specialty}</span>
                <strong>{doctor.fullName}</strong>
                <small>{doctor.clinicName}</small>
                <span className="card-meta">
                  <MapPin size={15} />
                  {doctor.city}, {doctor.state}
                </span>
              </button>
            ))}
          </section>
        )}

        {activeView === 'admin' && (
          <form className="admin-panel" onSubmit={submitDoctor}>
            <div className="section-heading">
              <BadgePlus size={19} />
              <span>Doctor profile management</span>
            </div>
            <div className="admin-grid">
              <label>
                Full name
                <input value={adminDoctor.fullName} onChange={(event) => updateAdmin('fullName', event.target.value)} required />
              </label>
              <label>
                Specialty
                <select value={adminDoctor.specialty} onChange={(event) => updateAdmin('specialty', event.target.value)}>
                  {specialtyOptions.map((specialty) => (
                    <option key={specialty}>{specialty}</option>
                  ))}
                </select>
              </label>
              <label>
                Clinic
                <input value={adminDoctor.clinicName} onChange={(event) => updateAdmin('clinicName', event.target.value)} required />
              </label>
              <label>
                Phone
                <input value={adminDoctor.phone} onChange={(event) => updateAdmin('phone', event.target.value)} />
              </label>
              <label className="wide">
                Bio
                <textarea value={adminDoctor.bio} onChange={(event) => updateAdmin('bio', event.target.value)} required />
              </label>
              <label>
                Address
                <input value={adminDoctor.addressLine} onChange={(event) => updateAdmin('addressLine', event.target.value)} required />
              </label>
              <label>
                Postal code
                <input value={adminDoctor.postalCode} onChange={(event) => updateAdmin('postalCode', event.target.value)} required />
              </label>
              <label>
                Latitude
                <input type="number" step="0.0001" value={adminDoctor.latitude} onChange={(event) => updateAdmin('latitude', Number(event.target.value))} />
              </label>
              <label>
                Longitude
                <input type="number" step="0.0001" value={adminDoctor.longitude} onChange={(event) => updateAdmin('longitude', Number(event.target.value))} />
              </label>
              <label>
                Languages
                <input value={adminDoctor.languages.join(', ')} onChange={(event) => updateAdmin('languages', splitList(event.target.value))} />
              </label>
              <label>
                Insurance
                <input value={adminDoctor.acceptedInsurances.join(', ')} onChange={(event) => updateAdmin('acceptedInsurances', splitList(event.target.value))} />
              </label>
              <label>
                Tags
                <input value={adminDoctor.profileTags.join(', ')} onChange={(event) => updateAdmin('profileTags', splitList(event.target.value))} />
              </label>
              <label className="toggle-row">
                <input type="checkbox" checked={adminDoctor.acceptingNewPatients} onChange={(event) => updateAdmin('acceptingNewPatients', event.target.checked)} />
                New patients
              </label>
              <label className="toggle-row">
                <input type="checkbox" checked={adminDoctor.telehealth} onChange={(event) => updateAdmin('telehealth', event.target.checked)} />
                Telehealth
              </label>
            </div>
            <button className="primary-button" disabled={loading} type="submit">
              <BadgePlus size={18} />
              Save doctor
            </button>
          </form>
        )}
      </section>

      <aside className="doctor-detail">
        {selectedDoctor ? (
          <>
            <div className="detail-topline">
              <span>{selectedDoctor.specialty}</span>
              <strong>{selectedDoctor.rating?.toFixed(1) ?? 'New'}</strong>
            </div>
            <h2>{selectedDoctor.fullName}</h2>
            <p>{selectedDoctor.bio}</p>
            <div className="detail-list">
              <span>
                <MapPin size={17} />
                {selectedDoctor.addressLine}, {selectedDoctor.city}
              </span>
              {selectedDoctor.phone && (
                <a className="detail-link" href={phoneHref(selectedDoctor.phone)}>
                  <PhoneCall size={17} />
                  {selectedDoctor.phone}
                </a>
              )}
              <span>
                <Clock3 size={17} />
                {selectedDoctor.nextAvailable ?? 'Call to confirm'}
              </span>
              <span>
                <Languages size={17} />
                {selectedDoctor.languages.length ? selectedDoctor.languages.join(', ') : 'Call to confirm'}
              </span>
              <span>
                <ShieldCheck size={17} />
                {selectedDoctor.acceptedInsurances.length ? selectedDoctor.acceptedInsurances.join(', ') : 'Call to confirm'}
              </span>
              <span>
                <Search size={17} />
                {sourceLabel(selectedDoctor.externalProvider ?? selectedResult?.placesSource)}
              </span>
            </div>
            {selectedDoctor.externalUri && (
              <a className="map-link" href={selectedDoctor.externalUri} target="_blank" rel="noreferrer">
                <ExternalLink size={16} />
                Open map listing
              </a>
            )}
            {selectedResult && (
              <div className="match-rationale">
                <strong>Match rationale</strong>
                <p>{selectedResult.reason}</p>
                <small>{recommendation?.disclaimer}</small>
              </div>
            )}
            {selectedResult && recommendation && (
              <form className="feedback-panel" onSubmit={submitSelectedFeedback}>
                <strong>Match feedback</strong>
                <div className="feedback-controls">
                  <label>
                    Helpfulness
                    <select value={feedbackRating} onChange={(event) => setFeedbackRating(Number(event.target.value))}>
                      <option value={5}>5 - Strong match</option>
                      <option value={4}>4 - Useful</option>
                      <option value={3}>3 - Mixed</option>
                      <option value={2}>2 - Weak</option>
                      <option value={1}>1 - Not useful</option>
                    </select>
                  </label>
                  <label className="toggle-row">
                    <input type="checkbox" checked={feedbackWouldContact} onChange={(event) => setFeedbackWouldContact(event.target.checked)} />
                    Would contact
                  </label>
                </div>
                <textarea
                  value={feedbackComment}
                  onChange={(event) => setFeedbackComment(event.target.value)}
                  maxLength={1200}
                  placeholder="Optional note for validation"
                  aria-label="Feedback note"
                />
                <button className="secondary-button" type="submit">
                  <CheckCircle2 size={16} />
                  Save feedback
                </button>
                {feedbackStatus && <small aria-live="polite">{feedbackStatus}</small>}
              </form>
            )}
          </>
        ) : (
          <div className="empty-state">
            <Stethoscope size={38} />
            <span>No doctor selected</span>
          </div>
        )}
      </aside>
    </main>
  );
}

function splitList(value: string) {
  return value
    .split(',')
    .map((item) => item.trim())
    .filter(Boolean);
}

function roundCoordinate(value: number) {
  return Math.round(value * 10000) / 10000;
}

function formatLocation(option: LocationSearchResult) {
  return option.address ? `${option.label} - ${option.address}` : option.label;
}

function phoneHref(phone: string) {
  return `tel:${phone.replace(/[^\d+]/g, '')}`;
}

function sourceLabel(source?: string | null) {
  if (source === 'google-places') {
    return 'Google Places';
  }
  if (source === 'serpapi-google-maps') {
    return 'SerpAPI Google Maps';
  }
  if (source === 'manual-coordinates') {
    return 'manual coordinates';
  }
  return 'Stored provider';
}

function readGeolocationError(error: GeolocationPositionError) {
  if (error.code === error.PERMISSION_DENIED) {
    return 'Location permission was denied.';
  }
  if (error.code === error.POSITION_UNAVAILABLE) {
    return 'Current location is unavailable.';
  }
  if (error.code === error.TIMEOUT) {
    return 'Location request timed out.';
  }
  return 'Unable to read current location.';
}

function readError(error: unknown) {
  if (error instanceof TypeError && error.message === 'Failed to fetch') {
    return 'Backend API is offline. Start Spring Boot or Docker Compose to load live doctors.';
  }
  return error instanceof Error ? error.message : 'Unexpected request failure';
}

export default App;
