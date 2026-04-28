package com.example.ATSCIRCLE.Repository;




import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

import com.example.ATSCIRCLE.Models.UserManagement.Employee;
import com.example.ATSCIRCLE.Models.UserManagement.Employee.Status;
@Repository
public interface EmployeeRepository extends MongoRepository<Employee, String> {

    Optional<Employee> findByEmail(String email);

    List<Employee> findByOrganizationId(String organizationId);

    List<Employee> findByOrganizationIdAndStatus(String organizationId, Status status);
      long countByOrganizationIdAndStatus(String organizationId, Employee.Status status);
 
    @Query(value = "{ 'organizationId': ?0 }", count = true)
    long countEmployeesByOrganization(String organizationId);

    boolean existsByEmail(String email);
}
