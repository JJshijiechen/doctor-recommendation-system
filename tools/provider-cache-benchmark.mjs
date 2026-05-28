#!/usr/bin/env node

const apiBaseUrl = process.env.API_BASE_URL ?? 'http://127.0.0.1:8080';
const allowExternalLookup = process.env.ALLOW_EXTERNAL_LOOKUP === 'true';

const runId = new Date().toISOString().replace(/[-:.TZ]/g, '').slice(0, 14);

const firstPayload = {
  symptoms: 'chest pain and shortness of breath',
  latitude: 41.8781,
  longitude: -87.6298,
  radiusMiles: 20,
  insurance: `Benchmark-Aetna-${runId}`,
  language: 'English',
  telehealthPreferred: true
};

const secondPayload = {
  ...firstPayload,
  insurance: `Benchmark-Cigna-${runId}`
};

async function readMetric(name) {
  const response = await fetch(`${apiBaseUrl}/actuator/metrics/${name}`);
  if (!response.ok) {
    return null;
  }
  const payload = await response.json();
  return payload.measurements?.[0]?.value ?? null;
}

async function recommend(payload) {
  const startedAt = performance.now();
  const response = await fetch(`${apiBaseUrl}/api/recommendations`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload)
  });
  const elapsedMs = performance.now() - startedAt;
  if (!response.ok) {
    throw new Error(await response.text());
  }
  return {
    elapsedMs,
    response: await response.json()
  };
}

if (!allowExternalLookup) {
  console.log(JSON.stringify(
    {
      status: 'dry-run',
      reason: 'Set ALLOW_EXTERNAL_LOOKUP=true to run two recommendation requests. A cold Redis cache can consume up to two external provider searches for the two inferred specialties.',
      command: 'ALLOW_EXTERNAL_LOOKUP=true node tools/provider-cache-benchmark.mjs'
    },
    null,
    2
  ));
  process.exit(0);
}

const metricsBefore = {
  hits: await readMetric('doctor.provider.lookup.cache.hits'),
  misses: await readMetric('doctor.provider.lookup.cache.misses'),
  writes: await readMetric('doctor.provider.lookup.cache.writes')
};

const first = await recommend(firstPayload);
const second = await recommend(secondPayload);

const metricsAfter = {
  hits: await readMetric('doctor.provider.lookup.cache.hits'),
  misses: await readMetric('doctor.provider.lookup.cache.misses'),
  writes: await readMetric('doctor.provider.lookup.cache.writes')
};

console.log(JSON.stringify(
  {
    apiBaseUrl,
    note: 'Second request changes insurance to avoid full recommendation-response cache; provider lookup cache should still hit because provider, specialty, coordinates, and radius are unchanged.',
    firstLookup: {
      elapsedMs: Math.round(first.elapsedMs),
      recommendationCacheHit: first.response.cacheHit,
      resultCount: first.response.results.length,
      topProvider: first.response.results[0]?.doctor?.fullName ?? null,
      topSource: first.response.results[0]?.doctor?.externalProvider ?? null
    },
    repeatedProviderLookup: {
      elapsedMs: Math.round(second.elapsedMs),
      recommendationCacheHit: second.response.cacheHit,
      resultCount: second.response.results.length,
      topProvider: second.response.results[0]?.doctor?.fullName ?? null,
      topSource: second.response.results[0]?.doctor?.externalProvider ?? null
    },
    metricsBefore,
    metricsAfter,
    metricDelta: {
      hits: metricsAfter.hits == null || metricsBefore.hits == null ? null : metricsAfter.hits - metricsBefore.hits,
      misses: metricsAfter.misses == null || metricsBefore.misses == null ? null : metricsAfter.misses - metricsBefore.misses,
      writes: metricsAfter.writes == null || metricsBefore.writes == null ? null : metricsAfter.writes - metricsBefore.writes
    }
  },
  null,
  2
));
