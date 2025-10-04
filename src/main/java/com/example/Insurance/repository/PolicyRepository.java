package com.example.Insurance.repository;

import com.example.Insurance.Enums.PolicyStatus;
import com.example.Insurance.entity.Policy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PolicyRepository extends JpaRepository<Policy, Long> {
    List<Policy> findByStatus(PolicyStatus status);
    List<Policy> findByVehicleType(String vehicleType);
    List<Policy> findByStatusAndVehicleType(PolicyStatus status, String vehicleType);

}
