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

        System.out.println("FETCH 명령 처리 시작");
        MailboxReader reader = new MailboxReader(username);
        
        // 원래 명령어를 다시 조합
        String command = String.join(" ", Arrays.copyOfRange(parts, 1, parts.length));
        System.out.println("전체 명령어: " + command);

        // FETCH 명령어 파싱
        if (!command.startsWith("FETCH ")) {
            out.println(tag + " BAD Invalid command");
            return;
        }

        // FETCH 다음 부분 파싱
        command = command.substring(6).trim(); // "FETCH " 제거
        String sequenceNumber = command.split(" ")[0];
        String fetchItems = command.substring(sequenceNumber.length()).trim()
                                  .replaceAll("\\s+", ""); // 모든 공백 제거

        System.out.println("시퀀스 번호: " + sequenceNumber);
        System.out.println("Fetch 항목: " + fetchItems);
        
        if (fetchItems.equals("(BODY[HEADER.FIELDS(FROMSUBJECTDATE)])")) {
            List<MailInfo> mailList = reader.getMailList();
            System.out.println("가져온 메일 리스트 크기: " + mailList.size());

            if (!sequenceNumber.equals("1:*")) {
                int index = Integer.parseInt(sequenceNumber) - 1;
                if (index < mailList.size()) {
                    MailInfo mail = mailList.get(index);
                    out.println("* " + sequenceNumber + " FETCH (BODY[HEADER.FIELDS (FROM SUBJECT DATE)] {" + 
                        (mail.getId().length() + mail.getFrom().length() + mail.getSubject().length() + mail.getDate().length() + 100) + "}");
                    out.println("ID: " + mail.getId() + "\r\n" +
                               "From: " + mail.getFrom() + "\r\n" +
                               "Subject: " + mail.getSubject() + "\r\n" +
                               "Date: " + mail.getDate() + "\r\n\r\n)");
                }
            } else {
                for (int i = 0; i < mailList.size(); i++) {
                    MailInfo mail = mailList.get(i);
                    System.out.println("메일 정보 출력 중: " + (i + 1));
                    out.println("* " + (i + 1) + " FETCH (BODY[HEADER.FIELDS (FROM SUBJECT DATE)] {" + 
                        (mail.getId().length() + mail.getFrom().length() + mail.getSubject().length() + mail.getDate().length() + 100) + "}");
                    out.println("ID: " + mail.getId() + "\r\n" +
                               "From: " + mail.getFrom() + "\r\n" +
                               "Subject: " + mail.getSubject() + "\r\n" +
                               "Date: " + mail.getDate() + "\r\n\r\n)");
                }
            }
        } else if (fetchItems.equals("(BODY[])")) {
            // 메일 전체 내용 요청
            List<MailInfo> mailList = reader.getMailList();
            
            if (!sequenceNumber.equals("1:*")) {
                int index = Integer.parseInt(sequenceNumber) - 1;
                if (index < mailList.size()) {
                    MailInfo mail = mailList.get(index);
                    String content = reader.getMailContent(mail.getId());
                    if (content != null) {
                        out.println("* " + sequenceNumber + " FETCH (BODY[] {" + content.length() + "}");
                        out.println(content);
                        out.println(")");
                    }
                }
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
