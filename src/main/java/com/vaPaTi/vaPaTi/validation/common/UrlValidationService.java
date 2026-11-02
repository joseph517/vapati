package com.vaPaTi.vaPaTi.validation.common;

import com.vaPaTi.vaPaTi.exception.MessageException;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URISyntaxException;

// Shared rule for optional URL fields stored as plain strings (D6: external URLs only, no uploads).
// fieldLabel starts each error message (e.g. "Cover image")
@Service
public class UrlValidationService {

    // null, empty or blank -> null (the caller decides whether null means "clear"). Otherwise the trimmed URL,
    // which must parse as a URI with scheme http or https and a host
    public String normalizeOptionalUrl(String value, String fieldLabel, int maxLength) {
        if (value == null || value.isBlank()) {
            return null;
        }

        String trimmed = value.trim();
        if (trimmed.length() > maxLength) {
            throw new MessageException(fieldLabel + " must be at most " + maxLength + " characters");
        }

        if (!isHttpUrlWithHost(trimmed)) {
            throw new MessageException(fieldLabel + " must be a valid http or https URL");
        }

        return trimmed;
    }

    private boolean isHttpUrlWithHost(String url) {
        URI uri;
        try {
            uri = new URI(url);
        } catch (URISyntaxException e) {
            return false;
        }

        String scheme = uri.getScheme();
        boolean httpScheme = "http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme);
        String host = uri.getHost();
        return httpScheme && host != null && !host.isBlank();
    }
}
