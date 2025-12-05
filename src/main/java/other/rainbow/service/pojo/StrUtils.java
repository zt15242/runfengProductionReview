package other.rainbow.service.pojo;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class StrUtils {

    public static Boolean isNotBlank(String data) {
        if (data != null && !"".equals(data.trim())) {
            return true;
        }
        return false;
    }

    public static String dateTOString(Long date) {
        Instant instant = Instant.ofEpochMilli(date);
        LocalDateTime dateLocal = LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        return dateLocal.format(formatter);
    }

    public static String dateTimeTOString(Long date) {
        Instant instant = Instant.ofEpochMilli(date);
        LocalDateTime dateLocal = LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        return dateLocal.format(formatter);
    }
}
