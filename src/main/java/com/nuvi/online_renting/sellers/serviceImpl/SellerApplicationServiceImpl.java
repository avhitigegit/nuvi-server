package com.nuvi.online_renting.sellers.serviceImpl;

import com.nuvi.online_renting.auth.service.EmailService;
import com.nuvi.online_renting.common.enums.Role;
import com.nuvi.online_renting.common.enums.SellerStatus;
import com.nuvi.online_renting.common.exceptions.BadRequestException;
import com.nuvi.online_renting.common.exceptions.ForbiddenException;
import com.nuvi.online_renting.common.exceptions.ResourceNotFoundException;
import com.nuvi.online_renting.common.security.AuthenticationFacade;
import com.nuvi.online_renting.sellers.dto.SellerApplicationRequestDTO;
import com.nuvi.online_renting.sellers.dto.SellerApplicationResponseDTO;
import com.nuvi.online_renting.sellers.dto.SellerDecisionDTO;
import com.nuvi.online_renting.sellers.dto.SellerSuspensionDTO;
import com.nuvi.online_renting.sellers.model.SellerApplication;
import com.nuvi.online_renting.sellers.repository.SellerApplicationRepository;
import com.nuvi.online_renting.sellers.service.SellerApplicationService;
import com.nuvi.online_renting.users.model.User;
import com.nuvi.online_renting.users.repository.UserRepository;
import com.nuvi.online_renting.common.storage.S3StorageService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SellerApplicationServiceImpl implements SellerApplicationService {

    private static final Logger log = LoggerFactory.getLogger(SellerApplicationServiceImpl.class);

    private final SellerApplicationRepository sellerApplicationRepository;
    private final UserRepository userRepository;
    private final AuthenticationFacade authFacade;
    private final S3StorageService s3StorageService;
    private final EmailService emailService;

//    @Override
//    @Transactional
//    public SellerApplicationResponseDTO apply(Long userId, SellerApplicationRequestDTO sellerApplicationRequestDTO, List<String> uploadedDocUrls) {
//        User user = userRepository.findById(userId)
//                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
//
//        // If there's existing pending application, update it instead of creating new
//        List<SellerApplication> existing = sellerApplicationRepository.findByUserId(userId);
//        SellerApplication sellerApplication = existing.stream()
//                .filter(sp -> sp.getStatus() == SellerStatus.PENDING || sp.getStatus() == SellerStatus.REQUEST_INFO)
//                .findFirst().orElse(null);
//
//        if (sellerApplication == null) {
//            sellerApplication = new SellerApplication();
//            sellerApplication.setUser(user);
//        }
//
//        sellerApplication.setBusinessName(sellerApplicationRequestDTO.getBusinessName());
//        sellerApplication.setAddress(sellerApplicationRequestDTO.getAddress());
//        sellerApplication.setIdNumber(sellerApplicationRequestDTO.getIdNumber());
//        sellerApplication.setBankAccount(sellerApplicationRequestDTO.getBankAccount());
//        sellerApplication.setDocumentUrls(uploadedDocUrls);
//        sellerApplication.setStatus(SellerStatus.PENDING);
//
//        sellerApplication = sellerApplicationRepository.save(sellerApplication);
//        return sellerApplicationToSellerApplicationResponseDTO(sellerApplication);
//    }

    @Override
    @Transactional
    public SellerApplicationResponseDTO apply(Long userId, SellerApplicationRequestDTO sellerApplicationRequestDTO) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // If there's existing pending application, update it instead of creating new
        List<SellerApplication> existing = sellerApplicationRepository.findByUserId(userId);
        SellerApplication sellerApplication = existing.stream()
                .filter(sp -> sp.getStatus() == SellerStatus.PENDING || sp.getStatus() == SellerStatus.REQUEST_INFO)
                .findFirst().orElse(null);

        if (sellerApplication == null) {
            sellerApplication = new SellerApplication();
            sellerApplication.setUser(user);
        }

        sellerApplication.setBusinessName(sellerApplicationRequestDTO.getBusinessName());
        sellerApplication.setAddress(sellerApplicationRequestDTO.getAddress());
        sellerApplication.setIdNumber(sellerApplicationRequestDTO.getIdNumber());
        sellerApplication.setBankAccount(sellerApplicationRequestDTO.getBankAccount());
        sellerApplication.setStatus(SellerStatus.PENDING);

        sellerApplication = sellerApplicationRepository.save(sellerApplication);
        log.info("Seller application submitted by user {}", userId);
        // Owner creating their own application — mask sensitive fields
        return sellerApplicationToSellerApplicationResponseDTO(sellerApplication, false);
    }

    @Override
    @Transactional
    public void attachDocs(Long applicationId, List<String> uploadedDocUrls) {
        SellerApplication sellerApplication = sellerApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Seller Application not found"));

        // Append new docs to existing list
        List<String> existingDocs = sellerApplication.getDocumentUrls();
        if (existingDocs == null) {
            existingDocs = new ArrayList<>();
        }
        existingDocs.addAll(uploadedDocUrls);

        sellerApplication.setDocumentUrls(existingDocs);
        sellerApplicationRepository.save(sellerApplication);
    }

    @Override
    public SellerApplicationResponseDTO getById(Long id) {
        SellerApplication sellerApplication = sellerApplicationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found"));

        User currentUser = authFacade.getCurrentUser();
        boolean isAdmin = currentUser.getRole() == Role.ADMIN;
        boolean isOwner = sellerApplication.getUser().getId().equals(currentUser.getId());

        if (!isAdmin && !isOwner) {
            throw new ForbiddenException("You are not allowed to view this application");
        }

        return sellerApplicationToSellerApplicationResponseDTO(sellerApplication, isAdmin);
    }

    @Override
    public List<SellerApplicationResponseDTO> getMine(Long userId) {
        List<SellerApplication> apps = sellerApplicationRepository.findByUserId(userId);
        // Owner sees their own applications — sensitive fields are masked
        return apps.stream()
                .map(app -> sellerApplicationToSellerApplicationResponseDTO(app, false))
                .collect(Collectors.toList());
    }

    @Override
    public List<SellerApplicationResponseDTO> getByStatus(String status) {
        List<SellerApplication> apps;
        if (status == null) {
            apps = sellerApplicationRepository.findAll();
        } else {
            SellerStatus st = SellerStatus.valueOf(status.toUpperCase());
            apps = sellerApplicationRepository.findByStatus(st);
        }
        // Admin-only endpoint — return full unmasked data
        return apps.stream()
                .map(app -> sellerApplicationToSellerApplicationResponseDTO(app, true))
                .collect(Collectors.toList());
    }

    // admin decision
    @Override
    @Transactional
    public SellerApplicationResponseDTO decide(Long id, SellerDecisionDTO decision, String adminEmail) {
        SellerApplication sellerApplication = sellerApplicationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found"));
        sellerApplication.setStatus(decision.getStatus());
        sellerApplication.setAdminComment(decision.getComment());
        // if approved -> promote user to SELLER role and mark KYC as verified
        // if rejected -> revoke KYC verification
        User user = sellerApplication.getUser();
        if (decision.getStatus() == SellerStatus.APPROVED) {
            user.setRole(Role.SELLER);
            user.setKycVerified(true);
            log.info("KYC verified for user {}", user.getId());
        } else if (decision.getStatus() == SellerStatus.REJECTED) {
            user.setKycVerified(false);
            log.info("KYC revoked for user {}", user.getId());
        }
        userRepository.save(user);
        sellerApplication = sellerApplicationRepository.save(sellerApplication);
        log.info("Seller application {} decided by {}: status={}", id, adminEmail, decision.getStatus());

        if (decision.getStatus() == SellerStatus.APPROVED) {
            emailService.sendSellerApplicationApproved(user.getEmail(), user.getName());
        } else if (decision.getStatus() == SellerStatus.REJECTED) {
            emailService.sendSellerApplicationRejected(user.getEmail(), user.getName(), decision.getComment());
        }

        // Admin decision — return full unmasked data
        return sellerApplicationToSellerApplicationResponseDTO(sellerApplication, true);
    }

    @Override
    @Transactional
    public void suspendSeller(Long userId, SellerSuspensionDTO dto, String adminEmail) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (user.getRole() != Role.SELLER) {
            throw new BadRequestException("Only users with SELLER role can be suspended");
        }

        if (user.isSuspended()) {
            throw new BadRequestException("Seller is already suspended");
        }

        user.setSuspended(true);
        user.setSuspensionReason(dto.getReason());
        user.setSuspendedAt(LocalDateTime.now());
        user.setSuspendedBy(adminEmail);
        userRepository.save(user);
        log.info("Seller {} suspended by admin {} — reason: {}", userId, adminEmail, dto.getReason());
        emailService.sendSellerSuspended(user.getEmail(), user.getName(), dto.getReason());
    }

    @Override
    @Transactional
    public void unsuspendSeller(Long userId, String adminEmail) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (user.getRole() != Role.SELLER) {
            throw new BadRequestException("Only users with SELLER role can be unsuspended");
        }

        if (!user.isSuspended()) {
            throw new BadRequestException("Seller is not currently suspended");
        }

        user.setSuspended(false);
        user.setSuspensionReason(null);
        user.setSuspendedAt(null);
        user.setSuspendedBy(null);
        userRepository.save(user);
        log.info("Seller {} unsuspended by admin {}", userId, adminEmail);
        emailService.sendSellerUnsuspended(user.getEmail(), user.getName());
    }

    /**
     * Converts a SellerApplication to its response DTO.
     * isAdmin = true  → sensitive fields (idNumber, bankAccount) returned as-is
     * isAdmin = false → sensitive fields are masked for privacy
     */
    private SellerApplicationResponseDTO sellerApplicationToSellerApplicationResponseDTO(
            SellerApplication sellerApplication, boolean isAdmin) {

        SellerApplicationResponseDTO dto = new SellerApplicationResponseDTO();
        dto.setId(sellerApplication.getId());
        dto.setUserId(sellerApplication.getUser().getId());
        dto.setBusinessName(sellerApplication.getBusinessName());
        dto.setAddress(sellerApplication.getAddress());
        dto.setStatus(sellerApplication.getStatus());
        dto.setAdminComment(sellerApplication.getAdminComment());
        // documentUrls stored as S3 keys — convert to short-lived pre-signed URLs (15 min)
        if (sellerApplication.getDocumentUrls() != null) {
            dto.setDocumentUrls(sellerApplication.getDocumentUrls().stream()
                    .map(s3StorageService::generateDocUrl)
                    .collect(java.util.stream.Collectors.toList()));
        }
        dto.setCreatedAt(sellerApplication.getCreatedAt());
        dto.setUpdatedAt(sellerApplication.getUpdatedAt());

        if (isAdmin) {
            dto.setIdNumber(sellerApplication.getIdNumber());
            dto.setBankAccount(sellerApplication.getBankAccount());
        } else {
            dto.setIdNumber(mask(sellerApplication.getIdNumber()));
            dto.setBankAccount(maskBankAccount(sellerApplication.getBankAccount()));
        }

        return dto;
    }

    /** Shows only last 4 chars: "****1234" */
    private String maskBankAccount(String value) {
        if (value == null || value.length() <= 4) return "****";
        return "****" + value.substring(value.length() - 4);
    }

    /** Shows first 2 and last 2 chars: "72****45" */
    private String mask(String value) {
        if (value == null || value.length() <= 4) return "****";
        return value.substring(0, 2) + "****" + value.substring(value.length() - 2);
    }
}
