package at.fraihs.cookoff.shared.web.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ApiErrorBody(String code, String message, List<ApiErrorDetail> details, String requestId,
                            Instant timestamp) {

    public static ApiErrorBody of(String code, String message) {
        return of(code, message, List.of());
    }

    public static ApiErrorBody of(String code, String message, List<ApiErrorDetail> details) {
        return new ApiErrorBody(code, message, details, UUID.randomUUID().toString(), Instant.now());
    }
}
