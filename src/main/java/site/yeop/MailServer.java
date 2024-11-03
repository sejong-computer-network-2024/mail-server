package site.yeop;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.Scanner;
import java.util.UUID;

import org.xbill.DNS.Lookup;
import org.xbill.DNS.MXRecord;
import org.xbill.DNS.Record;
import org.xbill.DNS.Type;

public class MailServer {
    private static final int SMTP_PORT = 25;
    private static final int CONNECTION_TIMEOUT = 5000;
    private static final int SOCKET_TIMEOUT = 10000;

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        System.out.print("송신자 이메일 주소를 입력하세요: ");
        String senderEmail = scanner.nextLine();
        System.out.print("수신자 이메일 주소를 입력하세요: ");
        String recipientEmail = scanner.nextLine();
        System.out.print("메일 제목을 입력하세요: ");
        String subject = scanner.nextLine();
        System.out.print("메일 내용을 입력하세요: ");
        String content = scanner.nextLine();

        String smtpServer = getSmtpServer(recipientEmail);
        if (smtpServer == null) {
            System.out.println("메일 서버를 찾을 수 없습니다.");
            return;
        }

        try {
            boolean sent = sendMail(smtpServer, SMTP_PORT, senderEmail, recipientEmail, subject, content);
            if (!sent) {
                System.out.println("메일 전송에 실패했습니다.");
            }
        } catch (Exception e) {
            System.out.println("메일 전송 중 오류 발생: " + e.getMessage());
        }
    }

    private static boolean sendMail(String server, int port, String from, String to, String subject, String content) {
        Socket socket = new Socket();
        try {
            // 소켓 연결 시 타임아웃 설정
            socket.connect(new InetSocketAddress(server, port), CONNECTION_TIMEOUT);
            socket.setSoTimeout(SOCKET_TIMEOUT);  // 읽기 타임아웃 설정
            
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), false);

            // 초기 응답 확인
            String response = in.readLine();
            System.out.println("서버: " + response);
            if (!response.startsWith("220")) {
                throw new IOException("서버 연결 실패: " + response);
            }

            // EHLO 명령을 HELO로 변경
            System.out.println("클라이언트: HELO yeop.site");
            out.print("HELO yeop.site\r\n");
            out.flush();
            
            response = in.readLine();
            System.out.println("서버: " + response);
            if (!response.startsWith("250")) throw new IOException("HELO 실패");

            // 메일 전송
            System.out.println("클라이언트: MAIL FROM:<" + from + ">");
            out.print("MAIL FROM:<" + from + ">\r\n");
            out.flush();
            response = in.readLine();
            System.out.println("서버: " + response);
            if (!response.startsWith("250")) throw new IOException("MAIL FROM 실패");

            System.out.println("클라이언트: RCPT TO:<" + to + ">");
            out.print("RCPT TO:<" + to + ">\r\n");
            out.flush();
            response = in.readLine();
            System.out.println("서버: " + response);
            if (!response.startsWith("250")) throw new IOException("RCPT TO 실패");

            System.out.println("클라이언트: DATA");
            out.print("DATA\r\n");
            out.flush();
            response = in.readLine();
            System.out.println("서버: " + response);
            if (!response.startsWith("354")) throw new IOException("DATA 실패");

            // 메일 내용 전송
            String messageId = UUID.randomUUID().toString() + "@yeop.site";
            System.out.println("클라이언트: 메일 내용 전송 시작");
            out.print("From: " + from + "\r\n");
            out.print("To: " + to + "\r\n");
            out.print("Subject: " + subject + "\r\n");
            out.print("Message-ID: <" + messageId + ">\r\n");
            out.print("Content-Type: text/plain; charset=UTF-8\r\n");
            out.print("\r\n");
            out.print(content + "\r\n");
            out.println(".\r\n");
            out.flush();
            System.out.println("클라이언트: 메일 내용 전송 완료");

            response = in.readLine();
            System.out.println("서버: " + response);
            if (!response.startsWith("250")) throw new IOException("메일 전송 실패");

            System.out.println("클라이언트: QUIT");
            out.print("QUIT\r\n");
            out.flush();
            return true;
        } catch (SocketTimeoutException e) {
            System.out.println("타임아웃 발생 (" + server + ":" + port + "): " + e.getMessage());
            return false;
        } catch (Exception e) {
            System.out.println("전송 실패 (" + server + ":" + port + "): " + e.getMessage());
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

    private static String getSmtpServer(String email) {
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
