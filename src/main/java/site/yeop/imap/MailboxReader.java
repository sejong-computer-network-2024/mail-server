package site.yeop.imap;

import java.io.*;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MailboxReader {
    private final String userMailbox;
    private final String username;

    public MailboxReader(String username) {
        this.username = username;
        this.userMailbox = "mailbox/" + username.replace("@", "_at_") + "/";
        System.out.println("메일박스 경로: " + userMailbox);
    }

    public int getMessageCount() {
        File mailboxDir = new File(userMailbox);
        if (!mailboxDir.exists()) {
            System.out.println("메일박스 디렉토리가 존재하지 않음 (카운트)");
            return 0;
        }
        File[] files = mailboxDir.listFiles((dir, name) -> name.endsWith(".eml"));
        int count = files != null ? files.length : 0;
        System.out.println("총 메일 수: " + count);
        return count;
    }

    public List<MailInfo> getMailList() {
        List<MailInfo> mailList = new ArrayList<>();
        File mailboxDir = new File(userMailbox);

        if (!mailboxDir.exists()) {
            System.out.println("메일박스 디렉토리가 존재하지 않음: " + userMailbox);
            return mailList;
        }

        File[] files = mailboxDir.listFiles((dir, name) -> name.endsWith(".eml"));
        if (files == null) {
            System.out.println("메일 파일을 찾을 수 없음");
            return mailList;
        }

        System.out.println("발견된 메일 파일 수: " + files.length);

        for (File file : files) {
            try {
                System.out.println("메일 파일 읽는 중: " + file.getName());
                BufferedReader reader = new BufferedReader(new FileReader(file));
                String from = "";
                String subject = "";
                String date = "";
                String line;
                
                while ((line = reader.readLine()) != null && !line.isEmpty()) {
                    if (line.startsWith("From: ")) from = line.substring(6);
                    if (line.startsWith("Subject: ")) subject = line.substring(9);
                    if (line.startsWith("Date: ")) date = line.substring(6);
                }
                reader.close();
                
                mailList.add(new MailInfo(file.getName(), from, subject, date));
                System.out.println("메일 정보 추가됨: " + file.getName());
            } catch (IOException e) {
                System.err.println("메일 읽기 오류 (" + file.getName() + "): " + e.getMessage());
                e.printStackTrace();
            }
        }

        System.out.println("총 읽어온 메일 수: " + mailList.size());
        return mailList;
    }

    public String getMailContent(String messageId) {
        File mailFile = new File(userMailbox + messageId);
        if (!mailFile.exists()) {
            System.out.println("메일 파일을 찾을 수 없음: " + messageId);
            return null;
        }

        try {
            return new String(Files.readAllBytes(mailFile.toPath()));
        } catch (IOException e) {
            System.err.println("메일 읽기 오류: " + e.getMessage());
            return null;
        }
    }

    public boolean markMailAsDeleted(String sequenceNumber) {
        try {
            File mailboxDir = new File(userMailbox);
            if (!mailboxDir.exists()) {
                return false;
            }

            File[] files = mailboxDir.listFiles((dir, name) -> name.endsWith(".eml"));
            if (files == null) {
                return false;
            }

            Arrays.sort(files);
            
            try {
                Integer.parseInt(sequenceNumber);
            } catch (NumberFormatException e) {
                System.out.println("올바르지 않은 시퀀스 번호: " + sequenceNumber);
                return false;
            }

            int index = Integer.parseInt(sequenceNumber) - 1;
            if (index >= 0 && index < files.length) {
                File file = files[index];
                File deletedFile = new File(file.getPath() + ".deleted");
                return file.renameTo(deletedFile);
            }
            
            return false;
        } catch (Exception e) {
            System.err.println("메일 삭제 표시 중 오류: " + e.getMessage());
            return false;
        }
    }
}