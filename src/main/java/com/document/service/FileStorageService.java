package com.document.service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.document.exception.BadRequestException;
import com.document.exception.ResourceNotFoundException;

/** Stores files on a directory (a Docker volume / Kubernetes PVC in production). */
@Service
public class FileStorageService {

	public record StoredResult(String storedName, String sha256, long size) {
	}

	private final Path root;
	private final Set<String> allowedExtensions;

	public FileStorageService(@Value("${app.storage.dir}") String dir,
			@Value("${app.storage.allowed-extensions}") String allowed) throws IOException {
		this.root = Path.of(dir).toAbsolutePath().normalize();
		Files.createDirectories(root);
		this.allowedExtensions = Arrays.stream(allowed.split(",")).map(s -> s.trim().toLowerCase(Locale.ROOT))
				.filter(s -> !s.isEmpty()).collect(Collectors.toSet());
	}

	public StoredResult store(MultipartFile file) {
		String ext = extensionOf(file.getOriginalFilename());
		if (!allowedExtensions.contains(ext)) {
			throw new BadRequestException("File type not allowed. Allowed: " + String.join(", ", allowedExtensions));
		}
		String storedName = UUID.randomUUID() + "." + ext;
		Path target = root.resolve(storedName).normalize();
		if (!target.startsWith(root)) {
			throw new BadRequestException("Invalid file name");
		}
		try (InputStream raw = file.getInputStream()) {
			MessageDigest md = MessageDigest.getInstance("SHA-256");
			try (InputStream in = new DigestInputStream(raw, md)) {
				Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
			}
			return new StoredResult(storedName, HexFormat.of().formatHex(md.digest()), Files.size(target));
		} catch (IOException | NoSuchAlgorithmException e) {
			throw new IllegalStateException("Could not store file", e);
		}
	}

	public Resource load(String storedName) {
		Path p = root.resolve(storedName).normalize();
		if (!p.startsWith(root) || !Files.isReadable(p)) {
			throw new ResourceNotFoundException("File content not found");
		}
		return new FileSystemResource(p);
	}

	public void delete(String storedName) {
		try {
			Path p = root.resolve(storedName).normalize();
			if (p.startsWith(root)) {
				Files.deleteIfExists(p);
			}
		} catch (IOException e) {
			throw new IllegalStateException("Could not delete file", e);
		}
	}

	private static String extensionOf(String name) {
		if (name == null || !name.contains(".")) {
			throw new BadRequestException("File must have an extension");
		}
		return name.substring(name.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
	}
}
