package com.example.Insurance.repository;

import com.example.Insurance.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface UserClaimRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);
    Optional<User> findByNic(String nic);
    boolean existsByEmail(String email);
    boolean existsByNic(String nic);
}
