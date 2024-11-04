package site.yeop.ftp;

import java.io.*; // 입출력 클래스들을 사용하기 위해 import
import java.net.Socket; // 소켓 연결을 위해 import

public class FtpSession implements Runnable {
    private final Socket clientSocket; // 클라이언트 소켓을 저장하는 변수

    // 생성자: 클라이언트 소켓을 인자로 받아 초기화
    public FtpSession(Socket clientSocket) {
        this.clientSocket = clientSocket;
    }

    @Override
    public void run() { // Runnable 인터페이스의 run 메소드 구현
        try (
                // 클라이언트와의 입출력 스트림 설정
                BufferedReader clientIn = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                PrintWriter clientOut = new PrintWriter(new OutputStreamWriter(clientSocket.getOutputStream()), true)
        ) {
            clientOut.println("220 FTP Server Ready"); // 서버 준비 완료 메시지 전송

            String line; // 클라이언트로부터 받은 명령을 저장할 변수
            boolean inFileTransferMode = false; // 파일 전송 모드 플래그
            String fileName = null; // 전송받을 파일 이름
            String externalServerAddress = null; // 외부 서버 주소
            int externalServerPort = -1; // 외부 서버 포트 번호
            ByteArrayOutputStream fileData = new ByteArrayOutputStream(); // 파일 데이터를 저장할 바이트 배열 출력 스트림

            // 클라이언트로부터 명령을 계속 읽음
            while ((line = clientIn.readLine()) != null) {
                System.out.println("클라이언트로부터 수신: " + line); // 수신된 명령 출력

                if (!inFileTransferMode) { // 파일 전송 모드가 아닐 경우
                    if (line.startsWith("SEND")) {
                        // SEND 명령 처리: 외부 서버 주소 및 포트 번호 설정
                        String[] parts = line.split(" ");
                        externalServerAddress = parts[1]; // 외부 서버 주소
                        externalServerPort = Integer.parseInt(parts[2]); // 외부 서버 포트 번호
                        clientOut.println("200 External server address and port set."); // 설정 완료 메시지 전송
                    } else if (line.startsWith("STOR")) { // STOR 명령 확인
                        fileName = line.split(" ")[1]; // 파일 이름 추출
                        clientOut.println("150 File status okay; about to open data connection."); // 파일 전송 준비 메시지
                        inFileTransferMode = true; // 파일 전송 모드로 전환
                    } else if (line.equals("QUIT")) { // QUIT 명령 확인
                        clientOut.println("221 Goodbye"); // 종료 메시지 전송
                        break; // 루프 종료
                    }
                } else { // 파일 전송 모드일 경우
                    if (line.equals(".")) {  // 파일 전송 종료 시점
                        inFileTransferMode = false; // 파일 전송 모드 해제
                        saveFile(fileName, fileData.toByteArray()); // 파일 저장
                        clientOut.println("226 Closing data connection. File transfer successful."); // 전송 완료 메시지

                        // 외부 서버로 전송
                        sendFileToExternalServer(fileName, fileData.toByteArray(), externalServerAddress, externalServerPort);
                        fileData.reset(); // 파일 데이터 초기화
                    } else {
                        fileData.write(line.getBytes()); // 클라이언트가 보낸 데이터 저장
                        fileData.write("\r\n".getBytes());  // CRLF 추가 (텍스트 파일의 줄 끝 표시)
                    }
                }
            }
        } catch (IOException e) { // 입출력 예외 처리
            System.out.println("클라이언트 처리 중 오류: " + e.getMessage());
        } finally {
            try {
                clientSocket.close(); // 클라이언트 소켓 닫기
            } catch (IOException e) {
                System.out.println("클라이언트 소켓 닫기 실패: " + e.getMessage());
            }
        }
    }

    // 파일을 로컬에 저장하는 메소드
    private void saveFile(String fileName, byte[] data) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(new File(fileName))) { // 파일 출력 스트림 설정
            fos.write(data); // 데이터 기록
        }
        System.out.println("파일 저장 완료: " + fileName); // 저장 완료 메시지 출력
    }

    // 외부 서버로 파일을 전송하는 메소드
    private void sendFileToExternalServer(String fileName, byte[] fileData, String externalServerAddress, int externalServerPort) {
        // 외부 서버로 파일을 전송하는 메서드
        try (Socket socket = new Socket(externalServerAddress, externalServerPort); // 외부 서버 소켓 생성
             OutputStream outputStream = socket.getOutputStream()) {

            outputStream.write(fileData); // 파일 데이터 전송
            System.out.println("외부 서버로 파일 전송 완료: " + fileName); // 전송 완료 메시지
        } catch (IOException e) {
            System.out.println("외부 서버로 파일 전송 실패: " + e.getMessage()); // 오류 메시지 출력
        }
    }
}
