package com.document.client;

import java.util.Map;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "tender-client", url = "${tenderflow.service.url}")
public interface TenderClient {

	/** Used only to check that the tender exists and is visible to the caller (TenderFlow answers 404 otherwise). */
	@GetMapping("/api/tenders/{id}")
	Map<String, Object> getTender(@PathVariable("id") Long id, @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization);
}
