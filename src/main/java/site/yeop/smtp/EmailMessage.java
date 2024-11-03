package site.yeop.smtp;

public class EmailMessage {
    private String from;
    private String to;
    private String subject;
    private StringBuilder body;
    private boolean inDataMode;

    public EmailMessage() {
        this.body = new StringBuilder();
        this.inDataMode = false;
    }

    // Getters and setters
    public String getFrom() { return from; }
    public void setFrom(String from) { this.from = from; }
    public String getTo() { return to; }
    public void setTo(String to) { this.to = to; }
    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }
    public String getBody() { return body.toString(); }
    public void appendToBody(String line) { body.append(line).append("\r\n"); }
    public boolean isInDataMode() { return inDataMode; }
    public void setInDataMode(boolean inDataMode) { this.inDataMode = inDataMode; }
    public void clearBody() { body = new StringBuilder(); }
} 