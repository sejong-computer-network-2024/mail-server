package site.yeop.imap;

public class MailInfo {
    private final String id;
    private final String from;
    private final String subject;
    private final String date;

    public MailInfo(String id, String from, String subject, String date) {
        this.id = id;
        this.from = from;
        this.subject = subject;
        this.date = date;
    }

    public String getId() { return id; }
    public String getFrom() { return from; }
    public String getSubject() { return subject; }
    public String getDate() { return date; }
}
