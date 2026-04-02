package com.nuvi.online_renting.kyc.service;

import com.nuvi.online_renting.common.dto.PagedResponse;
import com.nuvi.online_renting.common.enums.KycDocumentType;
import com.nuvi.online_renting.common.enums.KycStatus;
import com.nuvi.online_renting.kyc.dto.KycResponseDTO;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface KycService {

    // User submits a KYC document
    KycResponseDTO submitKyc(KycDocumentType documentType, MultipartFile file);

    // User views their own KYC submissions
    List<KycResponseDTO> getMySubmissions();

    // Admin views all submissions, optionally filtered by status
    PagedResponse<KycResponseDTO> getAllSubmissions(KycStatus status, Pageable pageable);

    // Admin approves — sets user.kycVerified = true
    KycResponseDTO approveSubmission(Long id);

    // Admin rejects — sets submission status to REJECTED with a reason
    KycResponseDTO rejectSubmission(Long id, String reason);
}
