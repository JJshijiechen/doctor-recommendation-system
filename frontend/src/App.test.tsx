import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react';
import { afterEach, beforeEach, expect, it, vi } from 'vitest';
import App from './App';

beforeEach(() => {
  vi.stubGlobal(
    'fetch',
    vi.fn((url: string) => {
      const requestUrl = String(url);
      if (requestUrl.endsWith('/api/doctors')) {
        return Promise.resolve({
          ok: true,
          json: () =>
            Promise.resolve([
              {
                id: 1,
                fullName: 'Northwestern Memorial Hospital Cardiology',
                specialty: 'Cardiology',
                clinicName: 'Northwestern Memorial Hospital Cardiology',
                bio: 'Live provider listing imported from SerpAPI Google Maps for cardiology care.',
                addressLine: '675 N St Clair St',
                city: 'Chicago',
                state: 'IL',
                postalCode: '60611',
                latitude: 41.8949,
                longitude: -87.622,
                phone: '(312) 926-9000',
                rating: 4.3,
                yearsExperience: null,
                acceptingNewPatients: true,
                telehealth: true,
                nextAvailable: 'Call to confirm',
                externalProvider: 'serpapi-google-maps',
                externalId: 'test-serpapi-place',
                externalUri:
                  'https://www.google.com/maps/search/?api=1&query=Northwestern%20Memorial%20Hospital%20Cardiology',
                languages: ['English'],
                acceptedInsurances: ['Aetna'],
                profileTags: ['chest pain']
              }
            ])
        });
      }
      if (requestUrl.includes('/api/locations/search')) {
        return Promise.resolve({
          ok: true,
          json: () =>
            Promise.resolve([
              {
                label: "Boston Children's Hospital",
                address: '300 Longwood Ave, Boston, MA',
                latitude: 42.3371,
                longitude: -71.1056,
                source: 'serpapi-google-maps'
              }
            ])
        });
      }
      return Promise.resolve({
        ok: true,
        json: () => Promise.resolve([{ id: 1, name: 'Cardiology', description: 'Heart care' }])
      });
    })
  );
});

afterEach(() => {
  cleanup();
  vi.unstubAllGlobals();
});

it('renders the patient matching workspace', async () => {
  render(<App googleOAuthEnabled={false} />);

  expect(await screen.findByText('Patient-guided doctor lookup')).toBeInTheDocument();
  expect(screen.getByRole('button', { name: /find doctors/i })).toBeInTheDocument();
  expect(screen.getByRole('button', { name: /use current location/i })).toBeInTheDocument();
  expect(screen.getByText('Demo Patient')).toBeInTheDocument();
  expect(await screen.findByText('(312) 926-9000')).toBeInTheDocument();
});

it('searches a natural-language location and updates coordinates', async () => {
  render(<App googleOAuthEnabled={false} />);

  const locationInput = await screen.findByLabelText('Location');
  fireEvent.change(locationInput, { target: { value: "Boston Children's Hospital" } });
  fireEvent.click(screen.getByRole('button', { name: /search location/i }));

  await waitFor(() => {
    expect(screen.getByLabelText('Latitude')).toHaveValue(42.3371);
    expect(screen.getByLabelText('Longitude')).toHaveValue(-71.1056);
  });
  expect(screen.getByText(/Boston Children's Hospital/i)).toBeInTheDocument();
  expect(screen.getByText(/selected via SerpAPI Google Maps/i)).toBeInTheDocument();
});

it('fills latitude and longitude from browser geolocation', async () => {
  Object.defineProperty(navigator, 'geolocation', {
    configurable: true,
    value: {
      getCurrentPosition: vi.fn((success: PositionCallback) =>
        success({
          coords: {
            latitude: 42.123456,
            longitude: -88.987654,
            accuracy: 12,
            altitude: null,
            altitudeAccuracy: null,
            heading: null,
            speed: null,
            toJSON: () => ({})
          },
          timestamp: Date.now(),
          toJSON: () => ({})
        })
      )
    }
  });

  render(<App googleOAuthEnabled={false} />);

  fireEvent.click(screen.getAllByRole('button', { name: /use current location/i })[0]);

  await waitFor(() => {
    expect(screen.getByLabelText('Latitude')).toHaveValue(42.1235);
    expect(screen.getByLabelText('Longitude')).toHaveValue(-88.9877);
  });
  expect(screen.getByText('Location updated')).toBeInTheDocument();
});
