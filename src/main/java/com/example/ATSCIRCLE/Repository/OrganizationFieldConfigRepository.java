package com.example.ATSCIRCLE.Repository;

import com.example.ATSCIRCLE.Models.ATS.OrganizationFieldConfig;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface OrganizationFieldConfigRepository extends MongoRepository<OrganizationFieldConfig, String> {
    
    // Find configuration by organization ID
    Optional<OrganizationFieldConfig> findByOrganizationId(String organizationId);
    
    // Check if configuration exists for organization
    boolean existsByOrganizationId(String organizationId);
    
    // Delete configuration by organization ID
    void deleteByOrganizationId(String organizationId);
}