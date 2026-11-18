package com.document.controller;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.document.dto.FileResponse;
import com.document.entity.StoredFile;
import com.document.service.DocumentService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class DocumentController {

	private final DocumentService service;

	@PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	@PreAuthorize("hasAuthority('DOCUMENT_WRITE')")
	public ResponseEntity<FileResponse> upload(@RequestParam("file") MultipartFile file,
			@RequestParam("tenderId") Long tenderId, Authentication auth,
			@RequestHeader(HttpHeaders.AUTHORIZATION) String authorization) {
		return ResponseEntity.status(HttpStatus.CREATED).body(service.upload(file, tenderId, auth.getName(), authorization));
	}

	@GetMapping
	@PreAuthorize("hasAuthority('DOCUMENT_READ')")
	public List<FileResponse> list(@RequestParam("tenderId") Long tenderId) {
		return service.listByTender(tenderId);
	}

	@GetMapping("/{id}")
	@PreAuthorize("hasAuthority('DOCUMENT_READ')")
	public FileResponse meta(@PathVariable UUID id) {
		return service.toResponse(service.getMeta(id));
	}

	@GetMapping("/{id}/download")
	@PreAuthorize("hasAuthority('DOCUMENT_READ')")
	public ResponseEntity<Resource> download(@PathVariable UUID id) {
		StoredFile meta = service.getMeta(id);
		Resource body = service.content(meta);
		return ResponseEntity.ok()
				.contentType(MediaType.APPLICATION_OCTET_STREAM)   // never trust the uploaded content type
				.header("X-Content-Type-Options", "nosniff")
				.header(HttpHeaders.CONTENT_DISPOSITION,
						ContentDisposition.attachment().filename(meta.getOriginalName(), StandardCharsets.UTF_8).build().toString())
				.body(body);
	}

	@DeleteMapping("/{id}")
	@PreAuthorize("hasAuthority('DOCUMENT_DELETE')")
	public ResponseEntity<Void> delete(@PathVariable UUID id, Authentication auth) {
		service.delete(id, auth.getName());
		return ResponseEntity.noContent().build();
	}
}
