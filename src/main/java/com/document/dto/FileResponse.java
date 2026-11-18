package com.document.dto;

import java.time.LocalDateTime;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class FileResponse {

	private UUID id;
	private Long tenderId;
	private String originalName;
	private String contentType;
	private long sizeBytes;
	private String sha256;
	private String uploadedBy;
	private LocalDateTime uploadedAt;
}
