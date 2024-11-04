package site.yeop.auth;

import java.io.*;
import java.nio.file.*;
import java.util.HashMap;
import java.util.Map;

public class UserAuth {
    private static final String USER_FILE = "config/users.txt";
    private static Map<String, String> userCredentials = new HashMap<>();
    
    static {
        loadUsers();
    }
    
    private static void loadUsers() {
        try {
            // config 디렉토리가 없으면 생성
            Files.createDirectories(Paths.get("config"));
            
            // users.txt 파일이 없으면 생성하고 기본 사용자 추가
            File userFile = new File(USER_FILE);
            if (!userFile.exists()) {
                try (PrintWriter writer = new PrintWriter(new FileWriter(userFile))) {
                    writer.println("admin@yeop.site:admin123"); // 기본 사용자
                }
            }
            
            // 파일에서 사용자 정보 읽기
            Files.readAllLines(Paths.get(USER_FILE)).forEach(line -> {
                String[] parts = line.split(":");
                if (parts.length == 2) {
                    userCredentials.put(parts[0], parts[1]);
                }
            });
            
            System.out.println("사용자 정보가 로드되었습니다.");
        } catch (IOException e) {
            System.err.println("사용자 파일 로드 중 오류: " + e.getMessage());
        }
    }
    
    public static boolean authenticate(String username, String password) {
        String storedPassword = userCredentials.get(username);
        return storedPassword != null && storedPassword.equals(password);
    }
    
    public static void addUser(String username, String password) {
        try (PrintWriter writer = new PrintWriter(new FileWriter(USER_FILE, true))) {
            writer.println(username + ":" + password);
            userCredentials.put(username, password);
        } catch (IOException e) {
            System.err.println("사용자 추가 중 오류: " + e.getMessage());
        }
    }
} 