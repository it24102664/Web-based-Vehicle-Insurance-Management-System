
package com.example.Insurance.repository;

import com.example.Insurance.entity.FormField;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface FormFieldRepository extends JpaRepository<FormField, Long> {

    List<FormField> findByClaimFormIdOrderByFieldOrder(Long formId);

    void deleteByClaimFormId(Long formId);
}
