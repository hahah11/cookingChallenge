package at.fraihs.cookoff.shared.web;

public record ApiResponse<T>(T data, ApiMeta meta) {

    public static <T> ApiResponse<T> of(T data) {
        return new ApiResponse<>(data, ApiMeta.now());
    }
}
