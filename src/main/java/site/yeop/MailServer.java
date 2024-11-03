package site.yeop;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
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
    private static final int LOCAL_SMTP_PORT = 25; // 로컬 SMTP 서버 포트
    private static final int CONNECTION_TIMEOUT = 5000;
    private static final int SOCKET_TIMEOUT = 10000;

    public static void main(String[] args) {
        // SMTP 서버 시작
        startSmtpServer();
    }

    private static void startSmtpServer() {
        try (ServerSocket serverSocket = new ServerSocket(LOCAL_SMTP_PORT)) {
            System.out.println("SMTP 서버가 포트 " + LOCAL_SMTP_PORT + "에서 시작되었습니다.");
            
            while (true) {
                Socket clientSocket = serverSocket.accept();
                new Thread(() -> handleClient(clientSocket)).start();
            }
        } catch (IOException e) {
            System.out.println("서버 시작 실패: " + e.getMessage());
        }
    }

    private static void handleClient(Socket clientSocket) {
        try {
            BufferedReader clientIn = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            PrintWriter clientOut = new PrintWriter(new OutputStreamWriter(clientSocket.getOutputStream()), true);

            // 연결 성공 메시지 전송
            clientOut.println("220 yeop.site SMTP Server Ready");

            String senderEmail = null;
            String recipientEmail = null;
            StringBuilder messageContent = new StringBuilder();
            String line;
            boolean inDataMode = false;

            while ((line = clientIn.readLine()) != null) {
                System.out.println("클라이언트로부터 수신: " + line);

                if (!inDataMode) {
                    if (line.toUpperCase().startsWith("HELO") || line.toUpperCase().startsWith("EHLO")) {
                        clientOut.println("250 Hello");
                    } else if (line.toUpperCase().startsWith("MAIL FROM:")) {
                        senderEmail = extractEmail(line);
                        clientOut.println("250 Sender OK");
                    } else if (line.toUpperCase().startsWith("RCPT TO:")) {
                        recipientEmail = extractEmail(line);
                        clientOut.println("250 Recipient OK");
                    } else if (line.toUpperCase().equals("DATA")) {
                        clientOut.println("354 Start mail input; end with <CRLF>.<CRLF>");
                        inDataMode = true;
                    } else if (line.toUpperCase().equals("QUIT")) {
                        clientOut.println("221 Bye");
                        break;
                    }
                } else {
                    if (line.equals(".")) {
                        inDataMode = false;
                        // 외부 메일 서버로 전달
                        String smtpServer = getSmtpServer(recipientEmail);
                        if (smtpServer != null) {
                            boolean sent = sendMail(smtpServer, SMTP_PORT, senderEmail, recipientEmail, 
                                                 extractSubject(messageContent.toString()), 
                                                 extractBody(messageContent.toString()));
                            if (sent) {
                                clientOut.println("250 Message accepted for delivery");
                            } else {
                                clientOut.println("554 Transaction failed");
                            }
                        } else {
                            clientOut.println("554 No valid mail server found");
                        }
                        messageContent = new StringBuilder();
                    } else {
                        messageContent.append(line).append("\r\n");
                    }
                }
            }

        } catch (IOException e) {
            System.out.println("클라이언트 처리 중 오류: " + e.getMessage());
        } finally {
            try {
                clientSocket.close();
            } catch (IOException e) {
                System.out.println("클라이언트 소켓 닫기 실패: " + e.getMessage());
            }
        }
    }

    private static String extractEmail(String line) {
        int start = line.indexOf('<');
        int end = line.indexOf('>');
        if (start != -1 && end != -1) {
            return line.substring(start + 1, end);
        }
        return line.split(":")[1].trim();
    }

    private static String extractSubject(String message) {
        String[] lines = message.split("\r\n");
        for (String line : lines) {
            if (line.toLowerCase().startsWith("subject:")) {
                return line.substring(8).trim();
            }
        }
        return "";
    }

    private static String extractBody(String message) {
        int emptyLineIndex = message.indexOf("\r\n\r\n");
        if (emptyLineIndex != -1) {
            return message.substring(emptyLineIndex + 4);
        }
        return message;
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
