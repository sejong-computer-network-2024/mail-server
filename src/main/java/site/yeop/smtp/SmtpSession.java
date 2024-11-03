package site.yeop.smtp;

import java.io.*;
import java.net.Socket;

public class SmtpSession implements Runnable {
    private final Socket clientSocket;
    private final BufferedReader in;
    private final PrintWriter out;
    private final EmailMessage emailMessage;
    private final SmtpProtocolHandler protocolHandler;

    public SmtpSession(Socket clientSocket) throws IOException {
        this.clientSocket = clientSocket;
        this.in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        this.out = new PrintWriter(new OutputStreamWriter(clientSocket.getOutputStream()), true);
        this.emailMessage = new EmailMessage();
        this.protocolHandler = new SmtpProtocolHandler();
    }

    @Override
    public void run() {
        try {
            out.println("220 yeop.site SMTP Server Ready");
            
            String line;
            while ((line = in.readLine()) != null) {
                SmtpCommand command = protocolHandler.parseLine(line);
                SmtpResponse response = protocolHandler.handleCommand(command, emailMessage);
                
                out.println(response.getMessage());
                
                if (command.getType() == SmtpCommandType.QUIT) {
                    break;
                }
                
                if (response.shouldRelayMail()) {
                    relayMail();
                }
            }
        } catch (IOException e) {
            System.out.println("세션 처리 중 오류: " + e.getMessage());
        } finally {
            closeConnection();
        }
    }

    private void relayMail() {
        MailRelay relay = new MailRelay();
        boolean success = relay.sendMail(emailMessage);
        if (success) {
            out.println("250 Message accepted for delivery");
        } else {
            out.println("554 Transaction failed");
        }
    }

    private void closeConnection() {
        try {
            if (in != null) in.close();
            if (out != null) out.close();
            if (clientSocket != null) clientSocket.close();
        } catch (IOException e) {
            System.out.println("연결 종료 중 오류: " + e.getMessage());
        }
    }
} 