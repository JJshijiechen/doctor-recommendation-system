package com.doctorrecommendation.service;

import com.doctorrecommendation.domain.Specialty;
import com.doctorrecommendation.domain.SymptomTerm;
import com.doctorrecommendation.repository.SpecialtyRepository;
import com.doctorrecommendation.repository.SymptomTermRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class DataSeeder implements CommandLineRunner {

    private final SpecialtyRepository specialtyRepository;
    private final SymptomTermRepository symptomTermRepository;

    public DataSeeder(
            SpecialtyRepository specialtyRepository,
            SymptomTermRepository symptomTermRepository
    ) {
        this.specialtyRepository = specialtyRepository;
        this.symptomTermRepository = symptomTermRepository;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (specialtyRepository.count() > 0) {
            return;
        }

        Map<String, Specialty> specialties = new LinkedHashMap<>();
        specialty("Family Medicine", "Primary care for everyday symptoms, preventive care, and referrals.", specialties);
        specialty("Internal Medicine", "Adult primary care and chronic condition management.", specialties);
        specialty("Cardiology", "Heart, circulation, chest pain, and cardiac risk evaluation.", specialties);
        specialty("Pulmonology", "Breathing, asthma, cough, and lung concerns.", specialties);
        specialty("Dermatology", "Skin, hair, nail, acne, rash, and lesion care.", specialties);
        specialty("Neurology", "Headache, dizziness, numbness, tremor, and nerve symptoms.", specialties);
        specialty("Orthopedics", "Bone, joint, back, sports injury, and mobility care.", specialties);
        specialty("Gastroenterology", "Digestive, stomach, reflux, liver, and bowel concerns.", specialties);
        specialty("Psychiatry", "Medication-supported mental health care.", specialties);
        specialty("Obstetrics and Gynecology", "Pregnancy, reproductive, pelvic, and menstrual health.", specialties);
        specialty("Pediatrics", "Medical care for infants, children, and teens.", specialties);
        specialty("Endocrinology", "Diabetes, thyroid, hormone, and metabolism concerns.", specialties);
        specialty("Allergy and Immunology", "Allergy, asthma, immune, and sinus concerns.", specialties);
        specialty("Urgent Care", "Same-day evaluation for non-emergency acute symptoms.", specialties);

        symptom("chest pain", "pressure in chest,tight chest,heart pain", specialties.get("Cardiology"), 1.4);
        symptom("shortness of breath", "difficulty breathing,wheezing,breathless", specialties.get("Pulmonology"), 1.3);
        symptom("rash", "itchy skin,hives,eczema,acne", specialties.get("Dermatology"), 1.1);
        symptom("headache", "migraine,dizziness,numbness,tingling", specialties.get("Neurology"), 1.0);
        symptom("joint pain", "back pain,knee pain,shoulder pain,sprain", specialties.get("Orthopedics"), 1.0);
        symptom("abdominal pain", "stomach pain,nausea,vomiting,reflux", specialties.get("Gastroenterology"), 1.1);
        symptom("anxiety", "panic,depression,insomnia,stress", specialties.get("Psychiatry"), 0.9);
        symptom("pregnancy", "prenatal,pelvic pain,menstrual,period pain", specialties.get("Obstetrics and Gynecology"), 1.0);
        symptom("child fever", "infant fever,baby fever,pediatric fever", specialties.get("Pediatrics"), 1.2);
        symptom("diabetes", "blood sugar,thyroid,hormone", specialties.get("Endocrinology"), 1.0);
        symptom("allergy", "sneezing,sinus,asthma,allergic", specialties.get("Allergy and Immunology"), 1.0);
        symptom("fever", "cold,flu,cough,sore throat", specialties.get("Family Medicine"), 0.8);
    }

    private void specialty(String name, String description, Map<String, Specialty> specialties) {
        Specialty specialty = specialtyRepository.save(new Specialty(name, description));
        specialties.put(name, specialty);
    }

    private void symptom(String term, String synonyms, Specialty specialty, double weight) {
        symptomTermRepository.save(new SymptomTerm(term, synonyms, specialty, weight));
    }
}
