package com.careertrack.modules.recruitment.service;

import com.careertrack.common.exception.ResourceNotFoundException;
import com.careertrack.modules.recruitment.dto.JobApplicationDto;
import com.careertrack.modules.recruitment.entity.ApplicationStatus;
import com.careertrack.modules.recruitment.entity.JobApplication;
import com.careertrack.modules.recruitment.entity.JobOffer;
import com.careertrack.modules.recruitment.entity.JobOfferStatus;
import com.careertrack.modules.recruitment.repository.JobApplicationRepository;
import com.careertrack.modules.recruitment.repository.JobOfferRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RecruitmentServiceImpl implements RecruitmentService {

    private final JobApplicationRepository jobApplicationRepository;
    private final JobOfferRepository jobOfferRepository;
    private final JavaMailSender mailSender;

    @Value("${careertrack.mail.from:no-reply@careertrack.sn}")
    private String mailFrom;

    @Override
    public List<JobApplicationDto> getAllApplications() {
        return jobApplicationRepository.findAll().stream()
                .map(this::mapToDto)
                .toList();
    }

    @Override
    public List<JobApplicationDto> getApplicationsByStatus(ApplicationStatus status) {
        return jobApplicationRepository.findByStatus(status).stream()
                .map(this::mapToDto)
                .toList();
    }

    @Override
    @Transactional
    public JobApplicationDto updateApplicationStatus(UUID applicationId, ApplicationStatus newStatus) {
        JobApplication application = jobApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Candidature introuvable : " + applicationId));

        ApplicationStatus oldStatus = application.getStatus();
        application.setStatus(newStatus);
        JobApplication saved = jobApplicationRepository.save(application);

        log.info("Candidature {} déplacée de {} vers {}", applicationId, oldStatus, newStatus);

        // Envoi d'une notification email au candidat si transition importante
        sendCandidateEmailNotification(saved, newStatus);

        return mapToDto(saved);
    }

    @Override
    public List<JobOffer> getPublishedOffers() {
        return jobOfferRepository.findByStatus(JobOfferStatus.OPEN);
    }

    private void sendCandidateEmailNotification(JobApplication app, ApplicationStatus status) {
        if (app.getCandidate() == null || app.getCandidate().getEmail() == null) {
            return;
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(mailFrom);
            message.setTo(app.getCandidate().getEmail());
            message.setSubject("Mise à jour de votre candidature - CareerTrack");

            String content = switch (status) {
                case PRESELECTIONNE -> "Bonjour " + app.getCandidate().getFullName() + ",\n\nVotre candidature pour le poste \"" +
                        app.getJobOffer().getTitle() + "\" a été présélectionnée par l'équipe RH. Nous reviendrons vers vous très prochainement.";
                case ENTRETIEN -> "Bonjour " + app.getCandidate().getFullName() + ",\n\nNous avons le plaisir de vous convier à un entretien pour le poste \"" +
                        app.getJobOffer().getTitle() + "\". Notre équipe RH va vous contacter pour convenir d'un créneau.";
                case OFFRE -> "Bonjour " + app.getCandidate().getFullName() + ",\n\nFélicitations ! Une proposition d'embauche a été émise pour votre profil sur le poste \"" +
                        app.getJobOffer().getTitle() + "\".";
                case EMBAUCHE -> "Bonjour " + app.getCandidate().getFullName() + ",\n\nBienvenue au sein de l'équipe ! Votre embauche a été finalisée.";
                case REFUSE -> "Bonjour " + app.getCandidate().getFullName() + ",\n\nNous vous remercions pour l'intérêt porté à notre entreprise. Malgré la qualité de votre parcours, nous avons décidé de poursuivre avec un autre profil.";
                default -> null;
            };

            if (content != null) {
                message.setText(content + "\n\nCordialement,\nDirection des Ressources Humaines - CareerTrack");
                mailSender.send(message);
                log.info("Email de notification envoyé à {}", app.getCandidate().getEmail());
            }
        } catch (Exception e) {
            log.warn("Impossible d'envoyer l'email au candidat {} : {}", app.getCandidate().getEmail(), e.getMessage());
        }
    }

    private JobApplicationDto mapToDto(JobApplication app) {
        return JobApplicationDto.builder()
                .id(app.getId())
                .jobOfferId(app.getJobOffer() != null ? app.getJobOffer().getId() : null)
                .jobOfferTitle(app.getJobOffer() != null ? app.getJobOffer().getTitle() : null)
                .candidateId(app.getCandidate() != null ? app.getCandidate().getId() : null)
                .candidateFullName(app.getCandidate() != null ? app.getCandidate().getFullName() : null)
                .candidateEmail(app.getCandidate() != null ? app.getCandidate().getEmail() : null)
                .candidatePhone(app.getCandidate() != null ? app.getCandidate().getPhone() : null)
                .status(app.getStatus())
                .currentStep(app.getCurrentStep())
                .rating(app.getRating())
                .notes(app.getNotes())
                .cvUrl(app.getCandidate() != null ? app.getCandidate().getCvUrl() : null)
                .build();
    }
}
