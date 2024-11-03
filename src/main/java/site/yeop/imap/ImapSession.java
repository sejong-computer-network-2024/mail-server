package site.yeop.imap;

import java.io.*;
import java.net.Socket;
import java.util.List;

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
                    case "LIST":
                        handleList(tag);
                        break;
                    case "SELECT":
                        handleSelect(tag, parts);
                        break;
                    case "FETCH":
                        handleFetch(tag, parts);
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
        // 실제 환경에서는 적절한 인증 로직 구현 필요
        authenticated = true;
        out.println(tag + " OK LOGIN completed");
    }

    private void handleList(String tag) {
        if (!authenticated) {
            out.println(tag + " NO Please authenticate first");
            return;
        }

        out.println("* LIST (\\HasNoChildren) \"/\" \"INBOX\"");
        out.println(tag + " OK LIST completed");
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
        List<String> messages = reader.getMessages();

        for (int i = 0; i < messages.size(); i++) {
            out.println("* " + (i + 1) + " FETCH (BODY[] {" + messages.get(i).length() + "}");
            out.println(messages.get(i));
            out.println(")");
        }

        out.println(tag + " OK FETCH completed");
    }

    private void handleLogout(String tag) {
        out.println("* BYE IMAP server logging out");
        out.println(tag + " OK LOGOUT completed");
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
