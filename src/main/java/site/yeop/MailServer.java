package site.yeop;

import site.yeop.imap.ImapServer;
import site.yeop.smtp.SmtpServer;

public class MailServer {
    public static void main(String[] args) {
        // SMTP 서버 시작
        SmtpServer smtpServer = new SmtpServer();
        new Thread(smtpServer::start).start();
        
        // IMAP 서버 시작
        ImapServer imapServer = new ImapServer();
        new Thread(imapServer::start).start();
    }
}
