package site.yeop;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.text.SimpleDateFormat;
import java.util.UUID;

import org.xbill.DNS.Lookup;
import org.xbill.DNS.MXRecord;
import org.xbill.DNS.Record;
import org.xbill.DNS.Type;

import site.yeop.imap.ImapServer;

public class MailServer {
    private static final int SMTP_PORT = 25;
    private static final int LOCAL_SMTP_PORT = 25; // 로컬 SMTP 서버 포트
    private static final int CONNECTION_TIMEOUT = 5000;
    private static final int SOCKET_TIMEOUT = 10000;
    private static final String MAIL_STORAGE_PATH = "mailbox/";

    public static void main(String[] args) {
        // SMTP 서버 시작
        new Thread(() -> startSmtpServer()).start();
        
        // IMAP 서버 시작
        ImapServer imapServer = new ImapServer();
        new Thread(imapServer::start).start();
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
                        // 수신된 메일이 우리 도메인으로 온 것인지 확인
                        if (isLocalDomain(recipientEmail)) {
                            // 로컬 메일박스에 저장
                            saveMailToFile(recipientEmail, senderEmail, 
                                extractSubject(messageContent.toString()),
                                extractBody(messageContent.toString()));
                            clientOut.println("250 Message accepted for delivery");
                        } else {
                            // 외부 도메인일 경우 기존 코드 실행
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

    private static void saveMailToFile(String recipient, String sender, String subject, String content) {
        try {
            // 수신자의 메일박스 디렉토리 생성
            String userMailbox = MAIL_STORAGE_PATH + recipient.replace("@", "_at_") + "/";
            File mailboxDir = new File(userMailbox);
            if (!mailboxDir.exists()) {
                mailboxDir.mkdirs();
            }

            // 현재 시간과 UUID를 조합하여 고유한 파일명 생성
            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            String fileName = timestamp + "_" + UUID.randomUUID().toString() + ".eml";

            // 메일 내용을 파일로 저장
            File mailFile = new File(userMailbox + fileName);
            try (PrintWriter writer = new PrintWriter(new OutputStreamWriter(
                    new FileOutputStream(mailFile), StandardCharsets.UTF_8))) {
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

    private static boolean isLocalDomain(String email) {
        return email.toLowerCase().endsWith("@yeop.site");
    }
}
