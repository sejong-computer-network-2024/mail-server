package site.yeop.smtp;

public enum SmtpCommandType {
    HELO,
    MAIL_FROM,
    RCPT_TO,
    DATA,
    QUIT,
    UNKNOWN
} 