package com.document.service;

import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.document.client.AuditPublisher;
import com.document.client.TenderClient;
import com.document.dto.FileResponse;
import com.document.entity.StoredFile;
import com.document.exception.BadRequestException;
import com.document.exception.ResourceNotFoundException;
import com.document.repository.StoredFileRepository;
import com.document.service.FileStorageService.StoredResult;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DocumentService {

	private final StoredFileRepository repository;
	private final FileStorageService storage;
	private final TenderClient tenderClient;
	private final AuditPublisher audit;

	@Value("${app.tender-check.enabled:true}")
	private boolean tenderCheckEnabled;

	/** Uploads a file for a tender. TenderFlow (Feign, with the user's token) must know the tender, else 404. */
	public FileResponse upload(MultipartFile file, Long tenderId, String actor, String authorization) {
		if (file == null || file.isEmpty()) {
			throw new BadRequestException("File is empty");
		}
		if (tenderId == null || tenderId <= 0) {
			throw new BadRequestException("tenderId is required");
		}
		if (tenderCheckEnabled) {
			tenderClient.getTender(tenderId, authorization); // FeignException 404/401/403 handled by GlobalExceptionHandler
		}
		StoredResult stored = storage.store(file);
		try {
			StoredFile saved = repository.save(StoredFile.builder()
					.tenderId(tenderId)
					.originalName(cleanName(file.getOriginalFilename()))
					.contentType(file.getContentType())
					.sizeBytes(stored.size())
					.storedName(stored.storedName())
					.sha256(stored.sha256())
					.uploadedBy(actor)
					.build());
			audit.publish(actor, "FILE_UPLOADED", "TENDER", String.valueOf(tenderId),
					saved.getOriginalName() + " (" + saved.getSizeBytes() + " bytes)");
			return toResponse(saved);
		} catch (RuntimeException e) {
			storage.delete(stored.storedName()); // do not leave orphan files on disk
			throw e;
		}
	}

	public List<FileResponse> listByTender(Long tenderId) {
		return repository.findByTenderIdOrderByUploadedAtDesc(tenderId).stream().map(this::toResponse).toList();
	}

	public StoredFile getMeta(UUID id) {
		return repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("File not found with id " + id));
	}

	public Resource content(StoredFile meta) {
		return storage.load(meta.getStoredName());
	}

	public void delete(UUID id, String actor) {
		StoredFile f = getMeta(id);
		repository.delete(f);
		storage.delete(f.getStoredName());
		audit.publish(actor, "FILE_DELETED", "TENDER", String.valueOf(f.getTenderId()), f.getOriginalName());
	}

	private static String cleanName(String name) {
		String n = name == null ? "file" : name.replace("\\", "/");
		n = n.substring(n.lastIndexOf('/') + 1).replaceAll("[\\r\\n]", "_");
		return n.length() > 255 ? n.substring(n.length() - 255) : n;
	}

	public FileResponse toResponse(StoredFile f) {
		return FileResponse.builder().id(f.getId()).tenderId(f.getTenderId()).originalName(f.getOriginalName())
				.contentType(f.getContentType()).sizeBytes(f.getSizeBytes()).sha256(f.getSha256())
				.uploadedBy(f.getUploadedBy()).uploadedAt(f.getUploadedAt()).build();
	}
}
