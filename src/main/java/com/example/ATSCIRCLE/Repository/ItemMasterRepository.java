package com.example.ATSCIRCLE.Repository;

import com.example.ATSCIRCLE.Models.Invoice.ItemMaster;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface ItemMasterRepository extends MongoRepository<ItemMaster, String> {
    
    // Find all items by organization ID
    List<ItemMaster> findByOrganizationId(String organizationId);
    
    // Find all items by organization, ordered by last used (most recent first)
    List<ItemMaster> findByOrganizationIdOrderByLastUsedAtDesc(String organizationId);
    
    // Find specific item by organization, name and HSN
    Optional<ItemMaster> findByOrganizationIdAndItemNameAndHsnSac(
        String organizationId, String itemName, String hsnSac);
    
    // Search items by name (case-insensitive, partial match)
    List<ItemMaster> findByOrganizationIdAndItemNameContainingIgnoreCase(
        String organizationId, String itemName);
    
    // Search items by HSN/SAC
    List<ItemMaster> findByOrganizationIdAndHsnSac(
        String organizationId, String hsnSac);
    
    // Check if item exists
    boolean existsByOrganizationIdAndItemNameAndHsnSac(
        String organizationId, String itemName, String hsnSac);
    
    // Delete all items for an organization
    void deleteByOrganizationId(String organizationId);
}
