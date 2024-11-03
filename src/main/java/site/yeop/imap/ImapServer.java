package site.yeop.imap;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class ImapServer {
    private static final int IMAP_PORT = 143;

    public void start() {
        try (ServerSocket serverSocket = new ServerSocket(IMAP_PORT)) {
            System.out.println("IMAP 서버가 포트 " + IMAP_PORT + "에서 시작되었습니다.");

            while (true) {
                Socket clientSocket = serverSocket.accept();
                ImapSession session = new ImapSession(clientSocket);
                new Thread(session).start();
            }
        } catch (IOException e) {
            System.out.println("IMAP 서버 시작 실패: " + e.getMessage());
        }
    }
}
