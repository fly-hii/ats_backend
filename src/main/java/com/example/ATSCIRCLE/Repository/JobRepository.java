package com.example.ATSCIRCLE.Repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

import com.example.ATSCIRCLE.Models.ATS.Jobs;

@Repository
public interface JobRepository extends MongoRepository<Jobs, String> {

    // Find by organization context
    List<Jobs> findByOrganizationId(String organizationId);

    Optional<Jobs> findByIdAndOrganizationId(String id, String organizationId);

    Optional<Jobs> findByJobCodeAndOrganizationId(String jobCode, String organizationId);

    List<Jobs> findByJobStatusAndOrganizationId(String jobStatus, String organizationId);

    List<Jobs> findByClientIdAndOrganizationId(String clientId, String organizationId);

    List<Jobs> findByDepartmentAndOrganizationId(String department, String organizationId);

    @Query("{ 'organizationId': ?1, 'primarySkills': { $in: ?0 } }")
    List<Jobs> findByPrimarySkillsInAndOrganizationId(List<String> skills, String organizationId);

    @Query("{ 'organizationId': ?1, 'secondarySkills': { $in: ?0 } }")
    List<Jobs> findBySecondarySkillsInAndOrganizationId(List<String> skills, String organizationId);

    boolean existsByJobCodeAndOrganizationId(String jobCode, String organizationId);

    // Find by creator or assignee
  // Find Jobs where (createdBy = userId OR assignedTo = userId) AND organizationId = orgId
@Query("{ 'organizationId': ?1, $or: [ { 'createdBy': ?0 }, { 'assignedTo': ?0 } ] }")
List<Jobs> findByCreatedByOrAssignedToAndOrganizationId(String userId, String organizationId);


    List<Jobs> findByIdInAndOrganizationId(List<String> ids, String organizationId);
}