
package com.example.ATSCIRCLE.Repository;

import com.example.ATSCIRCLE.Models.ATS.ClientShareableLink;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ClientShareableLinkRepository extends MongoRepository<ClientShareableLink, String> {
    
    Optional<ClientShareableLink> findByToken(String token);
    
    List<ClientShareableLink> findByOrganizationId(String organizationId);
    
    List<ClientShareableLink> findByClientId(String clientId);
    
    List<ClientShareableLink> findByJobId(String jobId);
    
    List<ClientShareableLink> findByClientEmail(String clientEmail);
    
    List<ClientShareableLink> findByOrganizationIdAndClientId(String organizationId, String clientId);
    
    List<ClientShareableLink> findByOrganizationIdAndJobId(String organizationId, String jobId);
    
    List<ClientShareableLink> findByIsExpired(Boolean isExpired);
    
    List<ClientShareableLink> findByIsRevoked(Boolean isRevoked);
    
    List<ClientShareableLink> findByExpiresAtBefore(LocalDateTime dateTime);
    
    List<ClientShareableLink> findByOrganizationIdAndIsExpiredAndIsRevoked(
        String organizationId, Boolean isExpired, Boolean isRevoked);
}