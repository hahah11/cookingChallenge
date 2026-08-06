package at.fraihs.cookoff.shared.web.dto;

public record ApiErrorResponse(ApiErrorBody error) {

    public static ApiErrorResponse of(String code, String message) {
        return new ApiErrorResponse(ApiErrorBody.of(code, message));
    }
}
