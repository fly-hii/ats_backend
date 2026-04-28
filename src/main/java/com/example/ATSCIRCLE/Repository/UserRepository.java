package com.example.ATSCIRCLE.Repository;

import com.example.ATSCIRCLE.Models.Users;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends MongoRepository<Users, String> {
    Optional<Users> findByEmail(String email);
     List<Users> findByRole(String role);
    
}
