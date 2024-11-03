package site.yeop.smtp;

import org.xbill.DNS.Lookup;
import org.xbill.DNS.MXRecord;
import org.xbill.DNS.Record;
import org.xbill.DNS.Type;

public class DnsResolver {
    public static String getSmtpServer(String email) {
        String domain = email.substring(email.indexOf("@") + 1);
        try {
            Record[] records = new Lookup(domain, Type.MX).run();
            if (records != null && records.length > 0) {
                MXRecord mxRecord = (MXRecord) records[0];
                return mxRecord.getTarget().toString();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
