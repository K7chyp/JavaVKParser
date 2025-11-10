package service;

public class VkApiException extends Exception {
    private final int errorCode;

    public VkApiException(String message, int errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    public int getErrorCode() {
        return errorCode;
    }
}