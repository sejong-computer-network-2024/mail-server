package site.yeop.smtp;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class SmtpServer {
    private static final int LOCAL_SMTP_PORT = 25;

    public void start() {
        try (ServerSocket serverSocket = new ServerSocket(LOCAL_SMTP_PORT)) {
            System.out.println("SMTP 서버가 포트 " + LOCAL_SMTP_PORT + "에서 시작되었습니다.");

            while (true) {
                Socket clientSocket = serverSocket.accept();
                new Thread(new SmtpSession(clientSocket)).start();
            }
        } catch (IOException e) {
            System.out.println("서버 시작 실패: " + e.getMessage());
        }
    }
}
