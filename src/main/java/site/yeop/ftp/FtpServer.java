package site.yeop.ftp;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class FtpServer {
    private static final int FTP_PORT = 21;

    public void start(){
        try(ServerSocket serverSocket = new ServerSocket(FTP_PORT)) {
            System.out.println("FTP 서버가 포트 " + FTP_PORT + "에서 시작되었습니다.");

            while(true){
                Socket clientSocket = serverSocket.accept();
                new Thread(new FtpSession(clientSocket)).start();
            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
