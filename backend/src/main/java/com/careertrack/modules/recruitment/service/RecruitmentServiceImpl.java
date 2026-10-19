package com.careertrack.modules.recruitment.service;

import com.careertrack.common.exception.ConflictException;
import com.careertrack.common.exception.ResourceNotFoundException;
import com.careertrack.modules.employee.entity.Department;
import com.careertrack.modules.employee.entity.Position;
import com.careertrack.modules.employee.repository.DepartmentRepository;
import com.careertrack.modules.employee.repository.PositionRepository;
import com.careertrack.modules.notification.entity.Notification;
import com.careertrack.modules.notification.entity.NotificationChannel;
import com.careertrack.modules.notification.entity.NotificationType;
import com.careertrack.modules.notification.repository.NotificationRepository;
import com.careertrack.modules.recruitment.dto.JobApplicationCreateDto;
import com.careertrack.modules.recruitment.dto.JobApplicationDto;
import com.careertrack.modules.recruitment.dto.JobOfferCreateDto;
import com.careertrack.modules.recruitment.dto.JobOfferDto;
import com.careertrack.modules.recruitment.entity.*;
import com.careertrack.modules.recruitment.repository.CandidateRepository;
import com.careertrack.modules.recruitment.repository.JobApplicationRepository;
import com.careertrack.modules.recruitment.repository.JobOfferRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RecruitmentServiceImpl implements RecruitmentService {

    private final JobApplicationRepository jobApplicationRepository;
    private final JobOfferRepository jobOfferRepository;
    private final CandidateRepository candidateRepository;
    private final DepartmentRepository departmentRepository;
    private final PositionRepository positionRepository;
    private final NotificationRepository notificationRepository;
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

        // Envoi d'une notification email au candidat et journalisation dans la table notifications
        sendCandidateEmailNotification(saved, newStatus);

        return mapToDto(saved);
    }

    @Override
    public List<JobOffer> getPublishedOffers() {
        return jobOfferRepository.findByStatus(JobOfferStatus.OPEN);
    }

    @Override
    @Transactional
    public JobOfferDto createJobOffer(JobOfferCreateDto createDto) {
        Department department = null;
        if (createDto.getDepartmentId() != null) {
            department = departmentRepository.findById(createDto.getDepartmentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Département introuvable : " + createDto.getDepartmentId()));
        }

        Position position = null;
        if (createDto.getPositionId() != null) {
            position = positionRepository.findById(createDto.getPositionId())
                    .orElseThrow(() -> new ResourceNotFoundException("Poste introuvable : " + createDto.getPositionId()));
        }

        String code = createDto.getCode();
        if (code == null || code.isBlank()) {
            code = "OFF-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        }

        JobOffer offer = JobOffer.builder()
                .code(code)
                .title(createDto.getTitle())
                .department(department)
                .position(position)
                .contractType(createDto.getContractType())
                .description(createDto.getDescription())
                .requirements(createDto.getRequirements())
                .status(JobOfferStatus.OPEN)
                .openPositions(createDto.getOpenPositions() > 0 ? createDto.getOpenPositions() : 1)
                .closingDate(createDto.getClosingDate())
                .build();

        JobOffer saved = jobOfferRepository.save(offer);
        return mapOfferToDto(saved, 0);
    }

    @Override
    public List<JobOfferDto> getAllOffers() {
        return jobOfferRepository.findAll().stream()
                .map(offer -> {
                    int count = jobApplicationRepository.findByJobOfferId(offer.getId()).size();
                    return mapOfferToDto(offer, count);
                })
                .toList();
    }

    @Override
    public JobOfferDto getOfferById(UUID offerId) {
        JobOffer offer = jobOfferRepository.findById(offerId)
                .orElseThrow(() -> new ResourceNotFoundException("Offre d'emploi introuvable : " + offerId));
        int count = jobApplicationRepository.findByJobOfferId(offer.getId()).size();
        return mapOfferToDto(offer, count);
    }

    @Override
    @Transactional
    public JobApplicationDto createApplication(JobApplicationCreateDto createDto) {
        JobOffer offer = jobOfferRepository.findById(createDto.getJobOfferId())
                .orElseThrow(() -> new ResourceNotFoundException("Offre d'emploi introuvable : " + createDto.getJobOfferId()));

        Candidate candidate = candidateRepository.findByEmail(createDto.getEmail())
                .orElseGet(() -> candidateRepository.save(Candidate.builder()
                        .firstName(createDto.getFirstName())
                        .lastName(createDto.getLastName())
                        .email(createDto.getEmail())
                        .phone(createDto.getPhone())
                        .cvUrl(createDto.getCvUrl())
                        .linkedinUrl(createDto.getLinkedinUrl())
                        .source("Candidature en ligne")
                        .build()));

        // Vérifier si la candidature existe déjà pour cette offre
        boolean exists = jobApplicationRepository.findByJobOfferId(offer.getId()).stream()
                .anyMatch(app -> app.getCandidate() != null && app.getCandidate().getId().equals(candidate.getId()));
        if (exists) {
            throw new ConflictException("Une candidature existe déjà pour ce candidat sur cette offre d'emploi");
        }

        JobApplication application = JobApplication.builder()
                .jobOffer(offer)
                .candidate(candidate)
                .status(ApplicationStatus.RECU)
                .currentStep("INITIAL_REVIEW")
                .notes(createDto.getNotes())
                .build();

        JobApplication saved = jobApplicationRepository.save(application);
        return mapToDto(saved);
    }

    private void sendCandidateEmailNotification(JobApplication app, ApplicationStatus status) {
        if (app.getCandidate() == null || app.getCandidate().getEmail() == null) {
            return;
        }

        String offerTitle = app.getJobOffer() != null ? app.getJobOffer().getTitle() : "Poste CareerTrack";
        String candidateName = app.getCandidate().getFullName();

        String content = switch (status) {
            case PRESELECTIONNE -> "Bonjour " + candidateName + ",\n\nVotre candidature pour le poste \"" +
                    offerTitle + "\" a été présélectionnée par l'équipe RH. Nous reviendrons vers vous très prochainement.";
            case ENTRETIEN -> "Bonjour " + candidateName + ",\n\nNous avons le plaisir de vous convier à un entretien pour le poste \"" +
                    offerTitle + "\". Notre équipe RH va vous contacter pour convenir d'un créneau.";
            case OFFRE -> "Bonjour " + candidateName + ",\n\nFélicitations ! Une proposition d'embauche a été émise pour votre profil sur le poste \"" +
                    offerTitle + "\".";
            case EMBAUCHE -> "Bonjour " + candidateName + ",\n\nBienvenue au sein de l'équipe ! Votre embauche a été finalisée avec succès.";
            case REFUSE -> "Bonjour " + candidateName + ",\n\nNous vous remercions pour l'intérêt porté à notre entreprise. Malgré la qualité de votre parcours, nous avons décidé de poursuivre avec un autre profil pour cette fois-ci.";
            default -> null;
        };

        if (content != null) {
            String subject = "Mise à jour de votre candidature - " + offerTitle;
            String fullBody = content + "\n\nCordialement,\nDirection des Ressources Humaines - CareerTrack";

            // 1. Envoi SMTP réel ou mock
            try {
                SimpleMailMessage message = new SimpleMailMessage();
                message.setFrom(mailFrom);
                message.setTo(app.getCandidate().getEmail());
                message.setSubject(subject);
                message.setText(fullBody);
                mailSender.send(message);
                log.info("Email de notification envoyé à {}", app.getCandidate().getEmail());
            } catch (Exception e) {
                log.warn("Impossible d'envoyer l'email SMTP au candidat {} (serveur non disponible ou hors-ligne) : {}",
                        app.getCandidate().getEmail(), e.getMessage());
            }

            // 2. Journalisation systématique dans la table notifications (conforme au cahier des charges)
            try {
                Notification notif = Notification.builder()
                        .recipient(null)
                        .recipientEmail(app.getCandidate().getEmail())
                        .title(subject)
                        .message(fullBody)
                        .channel(NotificationChannel.EMAIL)
                        .notificationType(NotificationType.RECRUITMENT)
                        .read(false)
                        .sentAt(Instant.now())
                        .metadata(String.format("{\"applicationId\":\"%s\",\"status\":\"%s\",\"candidateName\":\"%s\"}",
                                app.getId(), status.name(), candidateName))
                        .build();
                notificationRepository.save(notif);
                log.info("Notification archivée dans la table notifications pour {}", app.getCandidate().getEmail());
            } catch (Exception ex) {
                log.warn("Impossible d'archiver la notification en base : {}", ex.getMessage());
            }
        }
    }

    private JobOfferDto mapOfferToDto(JobOffer offer, int applicationsCount) {
        return JobOfferDto.builder()
                .id(offer.getId())
                .code(offer.getCode())
                .title(offer.getTitle())
                .departmentId(offer.getDepartment() != null ? offer.getDepartment().getId() : null)
                .departmentName(offer.getDepartment() != null ? offer.getDepartment().getName() : null)
                .positionId(offer.getPosition() != null ? offer.getPosition().getId() : null)
                .positionTitle(offer.getPosition() != null ? offer.getPosition().getTitle() : null)
                .contractType(offer.getContractType())
                .description(offer.getDescription())
                .requirements(offer.getRequirements())
                .status(offer.getStatus())
                .openPositions(offer.getOpenPositions())
                .closingDate(offer.getClosingDate())
                .applicationsCount(applicationsCount)
                .build();
    }

    private JobApplicationDto mapToDto(JobApplication app) {
        String appliedAt = app.getCreatedAt() != null
                ? DateTimeFormatter.ISO_INSTANT.format(app.getCreatedAt())
                : null;

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
                .appliedAt(appliedAt)
                .build();
    }
}
