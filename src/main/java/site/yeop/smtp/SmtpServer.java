package site.yeop.smtp;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class SmtpServer {
    private final int port;
    private boolean isRunning;
    
    public SmtpServer(int port) {
        this.port = port;
        this.isRunning = false;
    }

    public void start() {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("SMTP 서버가 포트 " + port + "에서 시작되었습니다.");
            isRunning = true;
            
            while (isRunning) {
                Socket clientSocket = serverSocket.accept();
                SmtpSession session = new SmtpSession(clientSocket);
                new Thread(session).start();
            }
        } catch (IOException e) {
            System.out.println("서버 시작 실패: " + e.getMessage());
        }
    }

    public void stop() {
        isRunning = false;
    }
} 