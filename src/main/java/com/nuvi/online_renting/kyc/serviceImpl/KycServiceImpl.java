package com.nuvi.online_renting.kyc.serviceImpl;

import com.nuvi.online_renting.auth.service.EmailService;
import com.nuvi.online_renting.common.dto.PagedResponse;
import com.nuvi.online_renting.common.enums.KycDocumentType;
import com.nuvi.online_renting.common.enums.KycStatus;
import com.nuvi.online_renting.common.exceptions.BadRequestException;
import com.nuvi.online_renting.common.exceptions.ResourceNotFoundException;
import com.nuvi.online_renting.common.security.AuthenticationFacade;
import com.nuvi.online_renting.common.storage.S3StorageService;
import com.nuvi.online_renting.common.validation.FileValidator;
import com.nuvi.online_renting.kyc.dto.KycResponseDTO;
import com.nuvi.online_renting.kyc.model.KycSubmission;
import com.nuvi.online_renting.kyc.repository.KycRepository;
import com.nuvi.online_renting.kyc.service.KycService;
import com.nuvi.online_renting.users.model.User;
import com.nuvi.online_renting.users.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class KycServiceImpl implements KycService {

    private final KycRepository kycRepository;
    private final UserRepository userRepository;
    private final AuthenticationFacade authFacade;
    private final S3StorageService s3StorageService;
    private final FileValidator fileValidator;
    private final EmailService emailService;

    public KycServiceImpl(KycRepository kycRepository,
                          UserRepository userRepository,
                          AuthenticationFacade authFacade,
                          S3StorageService s3StorageService,
                          FileValidator fileValidator,
                          EmailService emailService) {
        this.kycRepository = kycRepository;
        this.userRepository = userRepository;
        this.authFacade = authFacade;
        this.s3StorageService = s3StorageService;
        this.fileValidator = fileValidator;
        this.emailService = emailService;
    }

    @Override
    @Transactional
    public KycResponseDTO submitKyc(KycDocumentType documentType, MultipartFile file) {
        User user = authFacade.getCurrentUser();

        if (user.isKycVerified()) {
            throw new BadRequestException("Your KYC is already verified. No further submission is required.");
        }

        fileValidator.validateDocument(file);

        String originalName = file.getOriginalFilename() != null
                ? file.getOriginalFilename().replaceAll("[^a-zA-Z0-9._-]", "_")
                : "doc";
        String s3Key = "kyc/user_" + user.getId() + "_" + UUID.randomUUID() + "_" + originalName;
        s3StorageService.uploadFile(s3Key, file);

        KycSubmission submission = new KycSubmission();
        submission.setUser(user);
        submission.setDocumentType(documentType);
        submission.setDocumentKey(s3Key);
        submission.setStatus(KycStatus.PENDING);

        return toDTO(kycRepository.save(submission));
    }

    @Override
    @Transactional(readOnly = true)
    public List<KycResponseDTO> getMySubmissions() {
        Long userId = authFacade.getCurrentUser().getId();
        return kycRepository.findByUserId(userId).stream()
                .map(this::toDTO)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<KycResponseDTO> getAllSubmissions(KycStatus status, Pageable pageable) {
        Page<KycSubmission> page = kycRepository.findAllByStatus(status, pageable);
        return new PagedResponse<>(page.map(this::toDTO));
    }

    @Override
    @Transactional
    public KycResponseDTO approveSubmission(Long id) {
        KycSubmission submission = findOrThrow(id);

        if (submission.getStatus() != KycStatus.PENDING) {
            throw new BadRequestException("Only PENDING submissions can be approved");
        }

        submission.setStatus(KycStatus.APPROVED);
        submission.setReviewedAt(LocalDateTime.now());

        // Mark the user as KYC verified
        User user = submission.getUser();
        user.setKycVerified(true);
        userRepository.save(user);

        KycResponseDTO result = toDTO(kycRepository.save(submission));
        emailService.sendKycApproved(user.getEmail(), user.getName());
        return result;
    }

    @Override
    @Transactional
    public KycResponseDTO rejectSubmission(Long id, String reason) {
        KycSubmission submission = findOrThrow(id);

        if (submission.getStatus() != KycStatus.PENDING) {
            throw new BadRequestException("Only PENDING submissions can be rejected");
        }

        submission.setStatus(KycStatus.REJECTED);
        submission.setAdminComment(reason);
        submission.setReviewedAt(LocalDateTime.now());

        KycResponseDTO result = toDTO(kycRepository.save(submission));
        emailService.sendKycRejected(submission.getUser().getEmail(), submission.getUser().getName(), reason);
        return result;
    }

    private KycSubmission findOrThrow(Long id) {
        return kycRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("KYC submission not found with id " + id));
    }

    private KycResponseDTO toDTO(KycSubmission submission) {
        KycResponseDTO dto = new KycResponseDTO();
        dto.setId(submission.getId());
        dto.setUserId(submission.getUser().getId());
        dto.setUserName(submission.getUser().getName());
        dto.setDocumentType(submission.getDocumentType().name());
        dto.setDocumentUrl(s3StorageService.generateDocUrl(submission.getDocumentKey()));
        dto.setStatus(submission.getStatus().name());
        dto.setAdminComment(submission.getAdminComment());
        dto.setReviewedAt(submission.getReviewedAt());
        dto.setCreatedAt(submission.getCreatedAt());
        dto.setUpdatedAt(submission.getUpdatedAt());
        return dto;
    }
}
