package site.yeop.smtp;

public class SmtpProtocolHandler {
    public SmtpCommand parseLine(String line) {
        if (line == null) return new SmtpCommand(SmtpCommandType.UNKNOWN, "");
        
        String upperLine = line.toUpperCase();
        if (upperLine.startsWith("HELO") || upperLine.startsWith("EHLO")) {
            return new SmtpCommand(SmtpCommandType.HELO, line);
        } else if (upperLine.startsWith("MAIL FROM:")) {
            return new SmtpCommand(SmtpCommandType.MAIL_FROM, line);
        } else if (upperLine.startsWith("RCPT TO:")) {
            return new SmtpCommand(SmtpCommandType.RCPT_TO, line);
        } else if (upperLine.equals("DATA")) {
            return new SmtpCommand(SmtpCommandType.DATA, line);
        } else if (upperLine.equals("QUIT")) {
            return new SmtpCommand(SmtpCommandType.QUIT, line);
        }
        return new SmtpCommand(SmtpCommandType.UNKNOWN, line);
    }

    public SmtpResponse handleCommand(SmtpCommand command, EmailMessage email) {
        switch (command.getType()) {
            case HELO:
                return new SmtpResponse("250 Hello", false);
            case MAIL_FROM:
                email.setFrom(extractEmail(command.getLine()));
                return new SmtpResponse("250 Sender OK", false);
            case RCPT_TO:
                email.setTo(extractEmail(command.getLine()));
                return new SmtpResponse("250 Recipient OK", false);
            case DATA:
                email.setInDataMode(true);
                return new SmtpResponse("354 Start mail input; end with <CRLF>.<CRLF>", false);
            case QUIT:
                return new SmtpResponse("221 Bye", false);
            default:
                return new SmtpResponse("500 Unknown command", false);
        }
    }

    private String extractEmail(String line) {
        int start = line.indexOf('<');
        int end = line.indexOf('>');
        if (start != -1 && end != -1) {
            return line.substring(start + 1, end);
        }
        return line.split(":")[1].trim();
    }
} 