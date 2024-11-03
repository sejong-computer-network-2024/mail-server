package site.yeop.smtp;

public class SmtpResponse {
    private final String message;
    private final boolean shouldRelayMail;

    public SmtpResponse(String message, boolean shouldRelayMail) {
        this.message = message;
        this.shouldRelayMail = shouldRelayMail;
    }

    public String getMessage() { return message; }
    public boolean shouldRelayMail() { return shouldRelayMail; }
} 