package com.nuvi.online_renting.sellers.serviceImpl;

import com.nuvi.online_renting.common.enums.Role;
import com.nuvi.online_renting.common.enums.SellerStatus;
import com.nuvi.online_renting.common.exceptions.ResourceNotFoundException;
import com.nuvi.online_renting.sellers.dto.SellerApplicationRequestDTO;
import com.nuvi.online_renting.sellers.dto.SellerApplicationResponseDTO;
import com.nuvi.online_renting.sellers.dto.SellerDecisionDTO;
import com.nuvi.online_renting.sellers.model.SellerApplication;
import com.nuvi.online_renting.sellers.repository.SellerApplicationRepository;
import com.nuvi.online_renting.sellers.service.SellerApplicationService;
import com.nuvi.online_renting.users.model.User;
import com.nuvi.online_renting.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SellerApplicationServiceImpl implements SellerApplicationService {

    private final SellerApplicationRepository sellerApplicationRepository;
    private final UserRepository userRepository; // to fetch user

    @Override
    @Transactional
    public SellerApplicationResponseDTO apply(Long userId, SellerApplicationRequestDTO sellerApplicationRequestDTO, List<String> uploadedDocUrls) {
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
        sellerApplication.setDocumentUrls(uploadedDocUrls);
        sellerApplication.setStatus(SellerStatus.PENDING);

        sellerApplication = sellerApplicationRepository.save(sellerApplication);
        return sellerApplicationToSellerApplicationResponseDTO(sellerApplication);
    }

    @Override
    public SellerApplicationResponseDTO getById(Long id) {
        SellerApplication sellerApplication = sellerApplicationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found"));
        return sellerApplicationToSellerApplicationResponseDTO(sellerApplication);
    }

    @Override
    public List<SellerApplicationResponseDTO> getMine(Long userId) {
        List<SellerApplication> apps = sellerApplicationRepository.findByUserId(userId);
        return apps.stream()
                .map(this::sellerApplicationToSellerApplicationResponseDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<SellerApplicationResponseDTO> getByStatus(String status) {
        SellerStatus st = SellerStatus.valueOf(status.toUpperCase());
        List<SellerApplication> apps = sellerApplicationRepository.findByStatus(st);
        return apps.stream()
                .map(this::sellerApplicationToSellerApplicationResponseDTO)
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
        // if approved -> set user.role = SELLER
        if (decision.getStatus() == SellerStatus.APPROVED) {
            User user = sellerApplication.getUser();
            user.setRole(Role.SELLER);
            userRepository.save(user);
        }
        sellerApplication = sellerApplicationRepository.save(sellerApplication);
        return sellerApplicationToSellerApplicationResponseDTO(sellerApplication);
    }

    private SellerApplicationResponseDTO sellerApplicationToSellerApplicationResponseDTO(SellerApplication sellerApplication) {
        SellerApplicationResponseDTO sellerApplicationRequestDTO = new SellerApplicationResponseDTO();
        sellerApplicationRequestDTO.setId(sellerApplication.getId());
        sellerApplicationRequestDTO.setUserId(sellerApplication.getUser().getId());
        sellerApplicationRequestDTO.setBusinessName(sellerApplication.getBusinessName());
        sellerApplicationRequestDTO.setAddress(sellerApplication.getAddress());
        sellerApplicationRequestDTO.setIdNumber(sellerApplication.getIdNumber());
        sellerApplicationRequestDTO.setBankAccount(sellerApplication.getBankAccount());
        sellerApplicationRequestDTO.setDocumentUrls(sellerApplication.getDocumentUrls());
        sellerApplicationRequestDTO.setStatus(sellerApplication.getStatus());
        sellerApplicationRequestDTO.setAdminComment(sellerApplication.getAdminComment());
        sellerApplicationRequestDTO.setCreatedAt(sellerApplication.getCreatedAt());
        sellerApplicationRequestDTO.setUpdatedAt(sellerApplication.getUpdatedAt());
        return sellerApplicationRequestDTO;
    }
}
