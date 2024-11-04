package site.yeop.smtp;

import java.io.*;
import java.net.Socket;
import java.util.Base64;

public class SmtpSession implements Runnable {
    private final Socket clientSocket;

    public SmtpSession(Socket clientSocket) {
        this.clientSocket = clientSocket;
    }

    public class IdPasswordManager{
        private static final String IdPassword_FILE_PATH = "C:/test/mail-server/IdPassword.txt";//저장경로 수정 필요?

        public boolean checkvalue(String encodedUserId, String encodedPassword) {
            //String userId = new String(Base64.getDecoder().decode(encodedUserId));
            //String password = new String(Base64.getDecoder().decode(encodedPassword));

            String userId = encodedUserId;
            String password = encodedPassword;

            try(BufferedReader reader = new BufferedReader(new FileReader(IdPassword_FILE_PATH))){
                String line;
                while((line = reader.readLine())!= null){
                    String[] parts = line.split(":");//txt에 Id:Password\n형식으로 데이터 저장
                    if(parts.length == 2){
                        String storedUserId = parts[0].trim();
                        String storedPassword = parts[1].trim();
                        if(userId.equals(storedUserId) && password.equals(storedPassword)){
                            return true;
                        }
                    }
                }
            }catch(IOException e){
                System.out.println("Error reading IdPassword file");
            }
            return false;
        }
    }


    @Override
    public void run() {
        try (
                BufferedReader clientIn = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                PrintWriter clientOut = new PrintWriter(new OutputStreamWriter(clientSocket.getOutputStream()), true)
        ) {
            clientOut.println("220 yeop.site SMTP Server Ready");

            String senderEmail = null;
            String recipientEmail = null;
            StringBuilder messageContent = new StringBuilder();
            String line;
            boolean inDataMode = false;
            boolean login = false;
            int login_cnt = 0;

            while ((line = clientIn.readLine()) != null) {
                System.out.println("클라이언트로부터 수신: " + line);

                if (!inDataMode) {
                    if (line.toUpperCase().startsWith("HELO") || line.toUpperCase().startsWith("EHLO")) {
                        clientOut.println("250 Hello");
                    }else if (line.toUpperCase().equals("AUTH LOGIN")){
                        clientOut.println("334 UserId:");
                        String encodeUsername = clientIn.readLine();
                        clientOut.println("334 Password:");
                        String encodePassword = clientIn.readLine();

                        //입력된 id와 password가 일치하는지 확인 필요
                        IdPasswordManager idPasswordManager = new IdPasswordManager();
                        if(idPasswordManager.checkvalue(encodeUsername, encodePassword)){
                            login = true;
                            login_cnt = 0;
                            clientOut.println("235 Authentication successful");
                        }
                        else{
                            login_cnt++;
                            if(login_cnt >= 4){//로그인 시도가 5번을 넘었을 경우 연결을 종료
                                clientOut.println("421 Too many failed login attempts, closing connection.");
                                break;
                            }
                            clientOut.println("535 Authentication failed. Remaining attempts: "+(5-login_cnt)); //로그인을 시도가능한 회수를 같이 전달
                            continue;
                        }
                    }else if(line.toUpperCase().equals("QUIT")){
                        clientOut.println("221 Bye");
                        break;
                    }else if(!login){//login이 처리되지 않고 다른 명령어가 수신된 경우
                        clientOut.println("530 Authentication required");
                        continue;
                    }else if (line.toUpperCase().startsWith("MAIL FROM:")) {
                        senderEmail = MailParser.extractEmail(line);
                        clientOut.println("250 Sender OK");
                    } else if (line.toUpperCase().startsWith("RCPT TO:")) {
                        recipientEmail = MailParser.extractEmail(line);
                        clientOut.println("250 Recipient OK");
                    } else if (line.toUpperCase().equals("DATA")) {
                        clientOut.println("354 Start mail input; end with <CRLF>.<CRLF>");
                        inDataMode = true;
                    }
                } else {
                    if (line.equals(".")) {
                        inDataMode = false;
                        if (MailParser.isLocalDomain(recipientEmail)) {
                            MailStorage.saveMailToFile(recipientEmail, senderEmail, MailParser.extractSubject(messageContent.toString()), MailParser.extractBody(messageContent.toString()));
                            clientOut.println("250 Message accepted for delivery");
                        } else {
                            String smtpServer = DnsResolver.getSmtpServer(recipientEmail);
                            if (smtpServer != null) {
                                boolean sent = MailSender.sendMail(smtpServer, senderEmail, recipientEmail, MailParser.extractSubject(messageContent.toString()), MailParser.extractBody(messageContent.toString()));
                                clientOut.println(sent ? "250 Message accepted for delivery" : "554 Transaction failed");
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
}
