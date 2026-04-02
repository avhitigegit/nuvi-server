package com.nuvi.online_renting.recurring.service;

import com.nuvi.online_renting.common.dto.PagedResponse;
import com.nuvi.online_renting.recurring.dto.RecurringBookingRequestDTO;
import com.nuvi.online_renting.recurring.dto.RecurringBookingResponseDTO;
import org.springframework.data.domain.Pageable;

public interface RecurringBookingService {

    RecurringBookingResponseDTO createTemplate(RecurringBookingRequestDTO request);

    PagedResponse<RecurringBookingResponseDTO> getMyTemplates(Pageable pageable);

    RecurringBookingResponseDTO getTemplate(Long id);

    RecurringBookingResponseDTO pauseTemplate(Long id);

    RecurringBookingResponseDTO resumeTemplate(Long id);

    RecurringBookingResponseDTO cancelTemplate(Long id);
}
