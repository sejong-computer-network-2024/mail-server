package site.yeop;

import site.yeop.smtp.SmtpServer;

public class Main {
    public static void main(String[] args) {
        SmtpServer server = new SmtpServer(25);
        server.start();
    }
} 