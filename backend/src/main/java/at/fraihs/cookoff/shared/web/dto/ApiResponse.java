package at.fraihs.cookoff.shared.web.dto;

public record ApiResponse<T>(T data, ApiMeta meta) {

    public static <T> ApiResponse<T> of(T data) {
        return new ApiResponse<>(data, ApiMeta.now());
    }
}
