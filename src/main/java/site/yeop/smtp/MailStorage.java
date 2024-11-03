package site.yeop.smtp;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

public class MailStorage {
    private static final String MAIL_STORAGE_PATH = "mailbox/";

    public static void saveMailToFile(String recipient, String sender, String subject, String content) {
        try {
            String userMailbox = MAIL_STORAGE_PATH + recipient.replace("@", "_at_") + "/";
            File mailboxDir = new File(userMailbox);
            if (!mailboxDir.exists()) {
                mailboxDir.mkdirs();
            }

            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            String fileName = timestamp + "_" + UUID.randomUUID().toString() + ".eml";

            File mailFile = new File(userMailbox + fileName);
            try (PrintWriter writer = new PrintWriter(new OutputStreamWriter(new FileOutputStream(mailFile), StandardCharsets.UTF_8))) {
                writer.println("From: " + sender);
                writer.println("To: " + recipient);
                writer.println("Subject: " + subject);
                writer.println("Date: " + new Date());
                writer.println("Content-Type: text/plain; charset=UTF-8");
                writer.println();
                writer.println(content);
            }

            System.out.println("메일이 성공적으로 저장되었습니다: " + mailFile.getAbsolutePath());
        } catch (IOException e) {
            System.err.println("메일 저장 중 오류 발생: " + e.getMessage());
        }
    }
}
