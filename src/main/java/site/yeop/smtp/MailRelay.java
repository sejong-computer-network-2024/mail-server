package site.yeop.smtp;

import org.xbill.DNS.Lookup;
import org.xbill.DNS.MXRecord;
import org.xbill.DNS.Record;
import org.xbill.DNS.Type;
import java.io.*;
import java.net.*;

public class MailRelay {
    private static final int SMTP_PORT = 25;
    private static final int CONNECTION_TIMEOUT = 5000;
    private static final int SOCKET_TIMEOUT = 10000;

    public boolean sendMail(EmailMessage email) {
        String smtpServer = getSmtpServer(email.getTo());
        if (smtpServer == null) return false;

        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(smtpServer, SMTP_PORT), CONNECTION_TIMEOUT);
            socket.setSoTimeout(SOCKET_TIMEOUT);

            // 여기에 기존의 sendMail 메소드의 구현을 옮깁니다
            // 이메일 전송 로직...

            return true;
        } catch (Exception e) {
            System.out.println("메일 전송 실패: " + e.getMessage());
            return false;
        }
    }

    private String getSmtpServer(String email) {
        String domain = email.substring(email.indexOf("@") + 1);
        try {
            Record[] records = new Lookup(domain, Type.MX).run();
            if (records != null && records.length > 0) {
                MXRecord mxRecord = (MXRecord) records[0];
                return mxRecord.getTarget().toString();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
} 