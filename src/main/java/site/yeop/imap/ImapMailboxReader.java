package site.yeop.imap;

import java.io.*;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;

public class ImapMailboxReader {
    private static final String MAIL_STORAGE_PATH = "mailbox/";
    private final String userMailbox;

    public ImapMailboxReader(String username) {
        this.userMailbox = MAIL_STORAGE_PATH + username.replace("@", "_at_") + "/";
    }

    public int getMessageCount() {
        File mailboxDir = new File(userMailbox);
        if (!mailboxDir.exists()) return 0;
        return mailboxDir.list((dir, name) -> name.endsWith(".eml")).length;
    }

    public List<String> getMessages() {
        List<String> messages = new ArrayList<>();
        File mailboxDir = new File(userMailbox);
        
        if (!mailboxDir.exists()) {
            return messages;
        }

        File[] files = mailboxDir.listFiles((dir, name) -> name.endsWith(".eml"));
        if (files == null) return messages;

        for (File file : files) {
            try {
                String content = new String(Files.readAllBytes(file.toPath()));
                messages.add(content);
            } catch (IOException e) {
                System.err.println("메일 읽기 오류: " + e.getMessage());
            }
        }

        return messages;
    }
} 