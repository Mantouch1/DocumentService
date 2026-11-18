package com.document.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "stored_files")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StoredFile {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(nullable = false)
	private Long tenderId;

	@Column(nullable = false)
	private String originalName;

	private String contentType;

	private long sizeBytes;

	/** Name on disk (random UUID + extension), never derived from the client file name. */
	@Column(nullable = false, unique = true)
	private String storedName;

	@Column(length = 64)
	private String sha256;

	private String uploadedBy;

	@Column(nullable = false, updatable = false)
	private LocalDateTime uploadedAt;

	@PrePersist
	private void onCreate() {
		this.uploadedAt = LocalDateTime.now();
	}
}
