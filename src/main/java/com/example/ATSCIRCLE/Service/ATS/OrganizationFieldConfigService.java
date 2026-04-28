package com.example.ATSCIRCLE.Service.ATS;

import com.example.ATSCIRCLE.Models.ATS.OrganizationFieldConfig;
import com.example.ATSCIRCLE.Repository.OrganizationFieldConfigRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Optional;

import static com.example.ATSCIRCLE.Service.ATS.CandidateExcelHelper.createExcelTemplate;

@Service
public class OrganizationFieldConfigService {

    @Autowired
    private OrganizationFieldConfigRepository fieldConfigRepository;

    // Create or update field configuration for organization
    @Transactional
     public OrganizationFieldConfig saveOrUpdateFieldConfig(String organizationId, List<String> selectedFields, String userId) {
        Optional<OrganizationFieldConfig> existing = fieldConfigRepository.findByOrganizationId(organizationId);
        
        if (existing.isPresent()) {
            OrganizationFieldConfig config = existing.get();
            config.setSelectedFields(selectedFields);
            config.setUpdatedBy(userId);
            config.preUpdate();
            return fieldConfigRepository.save(config);
        } else {
            OrganizationFieldConfig newConfig = new OrganizationFieldConfig(organizationId, selectedFields);
            newConfig.setCreatedBy(userId);
            newConfig.setUpdatedBy(userId);
            newConfig.prePersist();
            return fieldConfigRepository.save(newConfig);
        }
    }
    // Get field configuration by organization ID
    public Optional<OrganizationFieldConfig> getFieldConfigByOrganization(String organizationId) {
        return fieldConfigRepository.findByOrganizationId(organizationId);
    }

    // Get selected fields for organization
    public List<String> getSelectedFieldsByOrganization(String organizationId) {
        Optional<OrganizationFieldConfig> config = fieldConfigRepository.findByOrganizationId(organizationId);
        return config.map(OrganizationFieldConfig::getSelectedFields).orElse(List.of());
    }

    // Check if organization has field configuration
    public boolean hasFieldConfig(String organizationId) {
        return fieldConfigRepository.existsByOrganizationId(organizationId);
    }

    // Delete field configuration
    @Transactional
    public void deleteFieldConfig(String organizationId) {
        fieldConfigRepository.deleteByOrganizationId(organizationId);
    }

    // Get all field configurations
    public List<OrganizationFieldConfig> getAllFieldConfigs() {
        return fieldConfigRepository.findAll();
    }

    // Generate Excel template with organization's selected fields
    public ByteArrayInputStream generateExcelTemplate(String organizationId) throws IOException {
        List<String> selectedFields = getSelectedFieldsByOrganization(organizationId);
        
        if (selectedFields.isEmpty()) {
            throw new RuntimeException("No field configuration found for organization. Please configure fields first.");
        }
        
        return createExcelTemplate(selectedFields);
    }
}