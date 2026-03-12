package com.nuvi.online_renting.disputes.service;

import com.nuvi.online_renting.common.dto.PagedResponse;
import com.nuvi.online_renting.common.enums.DisputeStatus;
import com.nuvi.online_renting.disputes.dto.DisputeRequestDTO;
import com.nuvi.online_renting.disputes.dto.DisputeResolveDTO;
import com.nuvi.online_renting.disputes.dto.DisputeResponseDTO;
import org.springframework.data.domain.Pageable;

public interface DisputeService {

    DisputeResponseDTO raiseDispute(DisputeRequestDTO dto);

    DisputeResponseDTO getDisputeById(Long id);

    PagedResponse<DisputeResponseDTO> getAllDisputes(DisputeStatus status, Pageable pageable);

    DisputeResponseDTO markUnderReview(Long id);

    DisputeResponseDTO resolveDispute(Long id, DisputeResolveDTO dto);

    DisputeResponseDTO rejectDispute(Long id, DisputeResolveDTO dto);
}
