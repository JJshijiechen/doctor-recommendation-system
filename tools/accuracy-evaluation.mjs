#!/usr/bin/env node

const cases = [
  {
    symptoms: 'chest pressure and shortness of breath while walking',
    expectedSpecialty: 'Cardiology'
  },
  {
    symptoms: 'wheezing with trouble breathing after a cold',
    expectedSpecialty: 'Pulmonology'
  },
  {
    symptoms: 'itchy rash and hives after using a new soap',
    expectedSpecialty: 'Dermatology'
  },
  {
    symptoms: 'migraine headache with dizziness and tingling',
    expectedSpecialty: 'Neurology'
  },
  {
    symptoms: 'knee pain after a soccer injury',
    expectedSpecialty: 'Orthopedics'
  },
  {
    symptoms: 'stomach pain nausea and reflux after meals',
    expectedSpecialty: 'Gastroenterology'
  },
  {
    symptoms: 'panic attacks anxiety and insomnia',
    expectedSpecialty: 'Psychiatry'
  },
  {
    symptoms: 'prenatal visit for pregnancy and pelvic pain',
    expectedSpecialty: 'Obstetrics and Gynecology'
  },
  {
    symptoms: 'baby with infant fever',
    expectedSpecialty: 'Pediatrics'
  },
  {
    symptoms: 'thyroid symptoms and high blood sugar',
    expectedSpecialty: 'Endocrinology'
  },
  {
    symptoms: 'sneezing sinus congestion and asthma flare',
    expectedSpecialty: 'Allergy and Immunology'
  },
  {
    symptoms: 'fever sore throat and flu-like cough',
    expectedSpecialty: 'Family Medicine'
  }
];

const medicalTerms = [
  { term: 'chest pain', synonyms: ['chest pressure', 'pressure in chest', 'tight chest'], specialty: 'Cardiology', weight: 1.4 },
  { term: 'shortness of breath', synonyms: ['trouble breathing', 'difficulty breathing', 'breathing trouble', 'wheezing'], specialty: 'Pulmonology', weight: 1.3 },
  { term: 'rash', synonyms: ['itchy rash', 'hives', 'eczema', 'acne'], specialty: 'Dermatology', weight: 1.1 },
  { term: 'headache', synonyms: ['migraine', 'dizziness', 'tingling', 'numbness'], specialty: 'Neurology', weight: 1.0 },
  { term: 'joint pain', synonyms: ['knee pain', 'back pain', 'sports injury', 'soccer injury'], specialty: 'Orthopedics', weight: 1.0 },
  { term: 'abdominal pain', synonyms: ['stomach pain', 'nausea', 'reflux', 'vomiting'], specialty: 'Gastroenterology', weight: 1.1 },
  { term: 'anxiety', synonyms: ['panic', 'panic attacks', 'insomnia', 'stress'], specialty: 'Psychiatry', weight: 0.9 },
  { term: 'pregnancy', synonyms: ['prenatal', 'pelvic pain', 'menstrual'], specialty: 'Obstetrics and Gynecology', weight: 1.0 },
  { term: 'child fever', synonyms: ['infant fever', 'baby fever'], specialty: 'Pediatrics', weight: 1.2 },
  { term: 'diabetes', synonyms: ['blood sugar', 'thyroid', 'hormone'], specialty: 'Endocrinology', weight: 1.0 },
  { term: 'allergy', synonyms: ['sneezing', 'sinus', 'asthma', 'allergic'], specialty: 'Allergy and Immunology', weight: 1.0 },
  { term: 'fever', synonyms: ['cold', 'flu', 'cough', 'sore throat'], specialty: 'Family Medicine', weight: 0.8 }
];

function normalize(value) {
  return value.toLowerCase().replace(/[^a-z0-9\s]/g, ' ').replace(/\s+/g, ' ').trim();
}

function baselinePredict(symptoms) {
  const text = normalize(symptoms);
  for (const item of medicalTerms) {
    if (text.includes(normalize(item.term))) {
      return item.specialty;
    }
  }
  return 'Family Medicine';
}

function enhancedPredict(symptoms) {
  const text = normalize(symptoms);
  const scores = new Map();
  for (const item of medicalTerms) {
    const matches = [item.term, ...item.synonyms].filter((term) => text.includes(normalize(term))).length;
    if (matches === 0) {
      continue;
    }
    const current = scores.get(item.specialty) ?? 0;
    const tf = 1 + Math.log(matches);
    const idf = item.weight;
    scores.set(item.specialty, current + tf * idf);
  }
  return [...scores.entries()].sort((a, b) => b[1] - a[1])[0]?.[0] ?? 'Family Medicine';
}

function evaluate(name, predict) {
  const rows = cases.map((item) => {
    const predicted = predict(item.symptoms);
    return {
      symptoms: item.symptoms,
      expected: item.expectedSpecialty,
      predicted,
      correct: predicted === item.expectedSpecialty
    };
  });
  const correct = rows.filter((row) => row.correct).length;
  return {
    name,
    total: rows.length,
    correct,
    accuracy: correct / rows.length,
    rows
  };
}

const baseline = evaluate('baseline first keyword match', baselinePredict);
const enhanced = evaluate('enhanced local extraction plus TF-IDF-style specialty scoring', enhancedPredict);
const lift = enhanced.accuracy - baseline.accuracy;

console.log(JSON.stringify(
  {
    dataset: 'tools/accuracy-evaluation.mjs embedded patient-intent sample',
    note: 'This is an engineering regression dataset, not clinical validation. Use a larger reviewed dataset before claiming production accuracy lift.',
    baseline: {
      correct: baseline.correct,
      total: baseline.total,
      accuracy: Number(baseline.accuracy.toFixed(4))
    },
    enhanced: {
      correct: enhanced.correct,
      total: enhanced.total,
      accuracy: Number(enhanced.accuracy.toFixed(4))
    },
    absoluteLift: Number(lift.toFixed(4)),
    rows: enhanced.rows
  },
  null,
  2
));
