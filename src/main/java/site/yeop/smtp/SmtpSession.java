package site.yeop.smtp;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class SmtpSession implements Runnable {
    private final Socket clientSocket;

    public SmtpSession(Socket clientSocket) {
        this.clientSocket = clientSocket;
    }

    @Override
    public void run() {
        try (
            BufferedReader clientIn = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            PrintWriter clientOut = new PrintWriter(new OutputStreamWriter(clientSocket.getOutputStream()), true)
        ) {
            clientOut.println("220 yeop.site SMTP Server Ready");

            String senderEmail = null;
            List<String> recipientEmails = new ArrayList<>();
            StringBuilder messageContent = new StringBuilder();
            String line;
            boolean inDataMode = false;

            while ((line = clientIn.readLine()) != null) {
                System.out.println("클라이언트로부터 수신: " + line);

                if (!inDataMode) {
                    if (line.toUpperCase().startsWith("HELO") || line.toUpperCase().startsWith("EHLO")) {
                        clientOut.println("250 Hello");
                    } else if (line.toUpperCase().startsWith("MAIL FROM:")) {
                        senderEmail = MailParser.extractEmail(line);
                        clientOut.println("250 Sender OK");
                    } else if (line.toUpperCase().startsWith("RCPT TO:")) {
                        String recipientEmail = MailParser.extractEmail(line);
                        recipientEmails.add(recipientEmail);
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
                        String rawMessage = messageContent.toString();
                        for (String recipientEmail : recipientEmails) {
                            if (MailParser.isLocalDomain(recipientEmail)) {
                                MailStorage.saveMailToFile(recipientEmail, senderEmail, rawMessage);
                                clientOut.println("250 Message accepted for delivery for " + recipientEmail);
                            } else {
                                String smtpServer = DnsResolver.getSmtpServer(recipientEmail);
                                if (smtpServer != null) {
                                    boolean sent = MailSender.sendMail(smtpServer, senderEmail, recipientEmail, rawMessage);
                                    clientOut.println(sent ? 
                                        "250 Message accepted for delivery for " + recipientEmail : 
                                        "554 Transaction failed for " + recipientEmail);
                                } else {
                                    clientOut.println("554 No valid mail server found for " + recipientEmail);
                                }
                            }
                        }
                        recipientEmails.clear();
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
