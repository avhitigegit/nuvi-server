package com.nuvi.online_renting.appeals.serviceImpl;

import com.nuvi.online_renting.appeals.dto.AppealRejectDTO;
import com.nuvi.online_renting.appeals.dto.AppealRequestDTO;
import com.nuvi.online_renting.appeals.dto.AppealResponseDTO;
import com.nuvi.online_renting.appeals.model.SuspensionAppeal;
import com.nuvi.online_renting.appeals.repository.SuspensionAppealRepository;
import com.nuvi.online_renting.appeals.service.SuspensionAppealService;
import com.nuvi.online_renting.auth.service.EmailService;
import com.nuvi.online_renting.common.dto.PagedResponse;
import com.nuvi.online_renting.common.enums.AppealStatus;
import com.nuvi.online_renting.common.enums.Role;
import com.nuvi.online_renting.common.exceptions.BadRequestException;
import com.nuvi.online_renting.common.exceptions.ForbiddenException;
import com.nuvi.online_renting.common.exceptions.ResourceNotFoundException;
import com.nuvi.online_renting.common.security.AuthenticationFacade;
import com.nuvi.online_renting.users.model.User;
import com.nuvi.online_renting.users.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class SuspensionAppealServiceImpl implements SuspensionAppealService {

    private final SuspensionAppealRepository appealRepository;
    private final UserRepository userRepository;
    private final AuthenticationFacade authFacade;
    private final EmailService emailService;

    public SuspensionAppealServiceImpl(SuspensionAppealRepository appealRepository,
                                       UserRepository userRepository,
                                       AuthenticationFacade authFacade,
                                       EmailService emailService) {
        this.appealRepository = appealRepository;
        this.userRepository = userRepository;
        this.authFacade = authFacade;
        this.emailService = emailService;
    }

    @Override
    @Transactional
    public AppealResponseDTO submitAppeal(AppealRequestDTO request) {
        User currentUser = authFacade.getCurrentUser();

        if (!currentUser.isSuspended()) {
            throw new BadRequestException("You can only submit an appeal if your account is currently suspended");
        }

        if (appealRepository.existsBySellerIdAndStatus(currentUser.getId(), AppealStatus.PENDING)) {
            throw new BadRequestException("You already have a pending appeal. Please wait for it to be reviewed before submitting another.");
        }

        SuspensionAppeal appeal = new SuspensionAppeal();
        appeal.setSeller(currentUser);
        appeal.setReason(request.getReason());
        appeal.setStatus(AppealStatus.PENDING);

        return toDTO(appealRepository.save(appeal));
    }

    @Override
    @Transactional(readOnly = true)
    public List<AppealResponseDTO> getMyAppeals() {
        User currentUser = authFacade.getCurrentUser();
        return appealRepository.findBySellerIdOrderByCreatedAtDesc(currentUser.getId())
                .stream().map(this::toDTO).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<AppealResponseDTO> getAllAppeals(AppealStatus status, Pageable pageable) {
        Page<SuspensionAppeal> page = (status != null)
                ? appealRepository.findByStatus(status, pageable)
                : appealRepository.findAll(pageable);
        return new PagedResponse<>(page.map(this::toDTO));
    }

    @Override
    @Transactional
    public AppealResponseDTO approveAppeal(Long appealId) {
        User admin = authFacade.getCurrentUser();
        SuspensionAppeal appeal = getAppealOrThrow(appealId);

        if (appeal.getStatus() != AppealStatus.PENDING) {
            throw new BadRequestException("Only PENDING appeals can be approved");
        }

        // Unsuspend the seller
        User seller = appeal.getSeller();
        seller.setSuspended(false);
        seller.setSuspensionReason(null);
        seller.setSuspendedAt(null);
        seller.setSuspendedBy(null);
        userRepository.save(seller);

        appeal.setStatus(AppealStatus.APPROVED);
        appeal.setReviewedAt(LocalDateTime.now());
        appeal.setReviewedBy(admin.getEmail());

        emailService.sendAppealApproved(seller.getEmail(), seller.getName());

        return toDTO(appealRepository.save(appeal));
    }

    @Override
    @Transactional
    public AppealResponseDTO rejectAppeal(Long appealId, AppealRejectDTO request) {
        User admin = authFacade.getCurrentUser();
        SuspensionAppeal appeal = getAppealOrThrow(appealId);

        if (appeal.getStatus() != AppealStatus.PENDING) {
            throw new BadRequestException("Only PENDING appeals can be rejected");
        }

        appeal.setStatus(AppealStatus.REJECTED);
        appeal.setAdminComment(request.getComment());
        appeal.setReviewedAt(LocalDateTime.now());
        appeal.setReviewedBy(admin.getEmail());

        User seller = appeal.getSeller();
        emailService.sendAppealRejected(seller.getEmail(), seller.getName(), request.getComment());

        return toDTO(appealRepository.save(appeal));
    }

    // -------------------------------------------------------------------------

    private SuspensionAppeal getAppealOrThrow(Long id) {
        return appealRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Appeal not found with id " + id));
    }

    private AppealResponseDTO toDTO(SuspensionAppeal appeal) {
        AppealResponseDTO dto = new AppealResponseDTO();
        dto.setId(appeal.getId());
        dto.setSellerId(appeal.getSeller().getId());
        dto.setSellerName(appeal.getSeller().getName());
        dto.setReason(appeal.getReason());
        dto.setStatus(appeal.getStatus());
        dto.setAdminComment(appeal.getAdminComment());
        dto.setReviewedAt(appeal.getReviewedAt());
        dto.setReviewedBy(appeal.getReviewedBy());
        dto.setCreatedAt(appeal.getCreatedAt());
        dto.setUpdatedAt(appeal.getUpdatedAt());
        return dto;
    }
}
