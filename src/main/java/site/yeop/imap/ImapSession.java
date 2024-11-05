package site.yeop.imap;

import java.io.*;
import java.net.Socket;
import java.util.Arrays;
import java.util.List;

import site.yeop.auth.UserAuth;

public class ImapSession implements Runnable {
    private final Socket clientSocket;
    private final BufferedReader in;
    private final PrintWriter out;
    private String username;
    private boolean authenticated;

    public ImapSession(Socket clientSocket) throws IOException {
        this.clientSocket = clientSocket;
        this.in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        this.out = new PrintWriter(clientSocket.getOutputStream(), true);
        this.authenticated = false;
    }

    @Override
    public void run() {
        try {
            out.println("* OK IMAP server ready");
            String line;
            String tag = "";

            while ((line = in.readLine()) != null) {
                System.out.println("[IMAP] 수신: " + line);
                String[] parts = line.split(" ");
                if (parts.length < 2) continue;

                tag = parts[0];
                String command = parts[1].toUpperCase();

                switch (command) {
                    case "LOGIN": 
                        handleLogin(tag, parts);
                        break;
                    case "SELECT": 
                        handleSelect(tag, parts);
                        break;
                    case "FETCH": 
                        handleFetch(tag, parts);
                        break;
                    case "STORE":
                        handleStore(tag, parts);
                        break;
                    case "LOGOUT": 
                        handleLogout(tag);
                        return;
                    default:
                        out.println(tag + " BAD Unknown command");
                }
            }
        } catch (IOException e) {
            System.out.println("IMAP 세션 오류: " + e.getMessage());
        } finally {
            closeConnection();
        }
    }

    private void handleLogin(String tag, String[] parts) {
        if (parts.length < 4) {
            out.println(tag + " BAD Invalid login command");
            return;
        }

        username = parts[2];
        String password = parts[3];
        
        if (UserAuth.authenticate(username, password)) {
            authenticated = true;
            out.println(tag + " OK LOGIN completed");
        } else {
            out.println(tag + " NO [AUTHENTICATIONFAILED] Invalid credentials");
            authenticated = false;
        }
    }

    private void handleSelect(String tag, String[] parts) {
        if (!authenticated) {
            out.println(tag + " NO Please authenticate first");
            return;
        }

        String mailbox = parts[2].replace("\"", "");
        if (!"INBOX".equals(mailbox)) {
            out.println(tag + " NO Mailbox does not exist");
            return;
        }

        MailboxReader reader = new MailboxReader(username);
        int messageCount = reader.getMessageCount();

        out.println("* " + messageCount + " EXISTS");
        out.println("* FLAGS (\\Seen \\Answered \\Flagged \\Deleted \\Draft)");
        out.println(tag + " OK [READ-WRITE] SELECT completed");
    }

    private void handleFetch(String tag, String[] parts) {
        if (!authenticated) {
            out.println(tag + " NO Please authenticate first");
            return;
        }

        MailboxReader reader = new MailboxReader(username);
        
        // 원래 명령어를 다시 조합
        String command = String.join(" ", Arrays.copyOfRange(parts, 1, parts.length));

        // FETCH 명령어 파싱
        if (!command.startsWith("FETCH ")) {
            out.println(tag + " BAD Invalid command");
            return;
        }

        command = command.substring(6).trim(); // "FETCH " 제거
        String sequenceNumber = command.split(" ")[0];

        List<MailInfo> mailList = reader.getMailList();

        if (sequenceNumber.contains(":")) {
            // 범위 요청
            String[] range = sequenceNumber.split(":");
            int start = Integer.parseInt(range[0]);
            int end = range[1].equals("*") ? mailList.size() : Integer.parseInt(range[1]);

            for (int i = start - 1; i < end; i++) {
                MailInfo mail = mailList.get(i);
                String headerContent = "From: " + mail.getFrom() + "\r\n" +
                                       "Subject: " + mail.getSubject() + "\r\n" +
                                       "Date: " + mail.getDate() + "\r\n\r\n";
                
                out.println("* " + (i + 1) + " FETCH (BODY[HEADER.FIELDS (FROM SUBJECT DATE)] {" + (headerContent.length()) + "}");
                out.println(headerContent);
            }
        } else {
            // 단일 요청
            int index = Integer.parseInt(sequenceNumber) - 1;
            
            if (index >= mailList.size()) {
                out.println(tag + " NO Message does not exist");
                return;
            }

            MailInfo mail = mailList.get(index);
            String content = reader.getMailContent(mail.getId());
            if (content != null) {
                out.println("* " + sequenceNumber + " FETCH (BODY[] {" + content.length() + "}");
                out.println(content);
                out.println(")");
            }
        }

        out.println(tag + " OK FETCH completed");
    }

    private void handleLogout(String tag) {
        out.println("* BYE IMAP server logging out");
        out.println(tag + " OK LOGOUT completed");
    }

    private void handleStore(String tag, String[] parts) {
        if (!authenticated) {
            out.println(tag + " NO Please authenticate first");
            return;
        }

        if (parts.length < 4) {
            out.println(tag + " BAD Invalid STORE command");
            return;
        }

        String sequenceNumber = parts[2];
        String flagSpec = String.join(" ", Arrays.copyOfRange(parts, 3, parts.length))
                               .toUpperCase();

        if (flagSpec.contains("FLAGS (\\DELETED)")) {
            MailboxReader reader = new MailboxReader(username);
            boolean success = reader.markMailAsDeleted(sequenceNumber);
            if (success) {
                out.println("* " + sequenceNumber + " FETCH (FLAGS (\\Deleted))");
                out.println(tag + " OK STORE completed");
            } else {
                out.println(tag + " NO STORE failed");
            }
        } else {
            out.println(tag + " BAD Unsupported flag");
        }
    }

    private void closeConnection() {
        try {
            if (in != null) in.close();
            if (out != null) out.close();
            if (clientSocket != null) clientSocket.close();
        } catch (IOException e) {
            System.out.println("IMAP 연결 종료 중 오류: " + e.getMessage());
        }
    }
}
