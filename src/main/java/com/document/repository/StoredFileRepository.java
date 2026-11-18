package com.document.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.document.entity.StoredFile;

public interface StoredFileRepository extends JpaRepository<StoredFile, UUID> {

	List<StoredFile> findByTenderIdOrderByUploadedAtDesc(Long tenderId);
}
