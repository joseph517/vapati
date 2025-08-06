package com.vaPaTi.vaPaTi.service;

import com.vaPaTi.vaPaTi.dtos.CampaignResponseDTO;
import com.vaPaTi.vaPaTi.dtos.CreateCampaignRequestDTO;
import com.vaPaTi.vaPaTi.entity.Campaign;
import com.vaPaTi.vaPaTi.entity.User;
import com.vaPaTi.vaPaTi.mapper.CampaignMapper;
import com.vaPaTi.vaPaTi.repository.CampaignRepository;
import com.vaPaTi.vaPaTi.repository.UserRepository;
import com.vaPaTi.vaPaTi.security.AuthenticatedUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CampaignService {

    private final CampaignRepository campaignRepository;
    private final AuthenticatedUserService authenticatedUserService;
    private final UserRepository userRepository;

    public List<CampaignResponseDTO> getAllCampaigns() {
        List<Campaign> campaigns = campaignRepository.findAll();

        return campaigns.stream()
                .map(CampaignMapper::toResponseDTO)
                .toList();
    }

    public CampaignResponseDTO createCampaign(CreateCampaignRequestDTO dto) {
        // 1️⃣ Obtener el userId desde el token
        Long userId = authenticatedUserService.getAuthenticatedUserId();

        // 2️⃣ Buscar la entidad User
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));

        // 3️⃣ Ajustar amountRaised si es null
        Double amountRaised = dto.getAmountRaised() != null ? dto.getAmountRaised() : 0;
        dto.setAmountRaised(amountRaised);

        // 4️⃣ Crear Campaign con Goal usando el mapper
        Campaign campaign = CampaignMapper.toEntity(dto, user);

        // 5️⃣ Guardar en la base de datos (CascadeType.ALL guardará también el Goal)
        Campaign savedCampaign = campaignRepository.save(campaign);

        // 6️⃣ Convertir a DTO de respuesta
        return CampaignMapper.toResponseDTO(savedCampaign);
    }

}

