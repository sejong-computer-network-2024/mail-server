package site.yeop.smtp;

public class SmtpCommand {
    private final SmtpCommandType type;
    private final String line;

    public SmtpCommand(SmtpCommandType type, String line) {
        this.type = type;
        this.line = line;
    }

    public SmtpCommandType getType() { return type; }
    public String getLine() { return line; }
} 