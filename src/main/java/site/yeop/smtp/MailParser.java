package site.yeop.smtp;

public class MailParser {
    private static final String LOCAL_DOMAIN = "yeop.site";

    public static String extractEmail(String line) {
        int start = line.indexOf('<');
        int end = line.indexOf('>');
        if (start != -1 && end != -1) {
            return line.substring(start + 1, end);
        }
        return line.split(":")[1].trim();
    }

    public static String extractSubject(String message) {
        String[] lines = message.split("\r\n");
        for (String line : lines) {
            if (line.toLowerCase().startsWith("subject:")) {
                return line.substring(8).trim();
            }
        }
        return "";
    }

    public static String extractBody(String message) {
        int emptyLineIndex = message.indexOf("\r\n\r\n");
        if (emptyLineIndex != -1) {
            return message.substring(emptyLineIndex + 4);
        }
        return message;
    }

    public static boolean isLocalDomain(String email) {
        return email.toLowerCase().endsWith("@" + LOCAL_DOMAIN);
    }
}
