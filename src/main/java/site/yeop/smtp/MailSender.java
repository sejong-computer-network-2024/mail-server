package site.yeop.smtp;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.UUID;

public class MailSender {
    private static final int SMTP_PORT = 25;
    private static final int CONNECTION_TIMEOUT = 5000;
    private static final int SOCKET_TIMEOUT = 10000;

    public static boolean sendMail(String server, String from, String to, String subject, String content) {
        Socket socket = new Socket();
        try {
            socket.connect(new InetSocketAddress(server, SMTP_PORT), CONNECTION_TIMEOUT);
            socket.setSoTimeout(SOCKET_TIMEOUT);

            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), false);

            if (!in.readLine().startsWith("220")) {
                throw new IOException("서버 연결 실패");
            }

            out.print("HELO yeop.site\r\n");
            out.flush();
            if (!in.readLine().startsWith("250")) {
                throw new IOException("HELO 실패");
            }

            out.print("MAIL FROM:<" + from + ">\r\n");
            out.flush();
            if (!in.readLine().startsWith("250")) {
                throw new IOException("MAIL FROM 실패");
            }

            out.print("RCPT TO:<" + to + ">\r\n");
            out.flush();
            if (!in.readLine().startsWith("250")) {
                throw new IOException("RCPT TO 실패");
            }

            out.print("DATA\r\n");
            out.flush();
            if (!in.readLine().startsWith("354")) {
                throw new IOException("DATA 실패");
            }

            String messageId = UUID.randomUUID().toString() + "@yeop.site";
            out.print("From: " + from + "\r\n");
            out.print("To: " + to + "\r\n");
            out.print("Subject: " + subject + "\r\n");
            out.print("Message-ID: <" + messageId + ">\r\n");
            out.print("Content-Type: text/plain; charset=UTF-8\r\n");
            out.print("\r\n");
            out.print(content + "\r\n");
            out.println(".\r\n");
            out.flush();

            if (!in.readLine().startsWith("250")) {
                throw new IOException("메일 전송 실패");
            }

            out.print("QUIT\r\n");
            out.flush();
            return true;
        } catch (SocketTimeoutException e) {
            System.out.println("타임아웃 발생 (" + server + ":" + SMTP_PORT + "): " + e.getMessage());
            return false;
        } catch (Exception e) {
            System.out.println("전송 실패 (" + server + ":" + SMTP_PORT + "): " + e.getMessage());
            return false;
        } finally {
            try {
                if (!socket.isClosed()) {
                    socket.close();
                }
            } catch (IOException e) {
                System.out.println("소켓 닫기 실패: " + e.getMessage());
            }
        }
    }
}
