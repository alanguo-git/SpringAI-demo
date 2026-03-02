package spring.ai.example.springaidemo.util;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class DateUtils {
    public static String getCurrentDate() {
        LocalDate now = LocalDate.now();
        return now.format(DateTimeFormatter.ofPattern("yyyy年MM月dd日"));
    }
}
