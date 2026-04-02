package com.nuvi.online_renting.appeals.service;

import com.nuvi.online_renting.appeals.dto.AppealRejectDTO;
import com.nuvi.online_renting.appeals.dto.AppealRequestDTO;
import com.nuvi.online_renting.appeals.dto.AppealResponseDTO;
import com.nuvi.online_renting.common.dto.PagedResponse;
import com.nuvi.online_renting.common.enums.AppealStatus;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface SuspensionAppealService {

    AppealResponseDTO submitAppeal(AppealRequestDTO request);

    List<AppealResponseDTO> getMyAppeals();

    PagedResponse<AppealResponseDTO> getAllAppeals(AppealStatus status, Pageable pageable);

    AppealResponseDTO approveAppeal(Long appealId);

    AppealResponseDTO rejectAppeal(Long appealId, AppealRejectDTO request);
}
