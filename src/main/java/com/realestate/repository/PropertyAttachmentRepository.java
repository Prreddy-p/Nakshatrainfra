package com.realestate.repository;

import com.realestate.model.PropertyAttachment;
import com.realestate.dto.PropertyAttachmentInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface PropertyAttachmentRepository extends JpaRepository<PropertyAttachment, String> {
    @Query("select new com.realestate.dto.PropertyAttachmentInfo(a.id, a.fileName, a.size, a.uploadedAt) from PropertyAttachment a where a.property.id = :propertyId order by a.uploadedAt, a.id")
    List<PropertyAttachmentInfo> list(@Param("propertyId") Long propertyId);
    @Query("select new com.realestate.dto.PropertyAttachmentInfo(a.id, a.fileName, a.size, a.uploadedAt) from PropertyAttachment a order by a.uploadedAt desc, a.id")
    List<PropertyAttachmentInfo> galleryCandidates();
    Optional<PropertyAttachment> findByIdAndProperty_Id(String id, Long propertyId);
}
