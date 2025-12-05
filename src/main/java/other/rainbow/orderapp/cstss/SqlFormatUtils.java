package other.rainbow.orderapp.cstss;

import java.util.List;

public class SqlFormatUtils {

    public static String joinInSql(List<String> list) {
        StringBuilder joinInSqlBuilder = new StringBuilder();
        if(list != null && list.size() > 0) {
            list.forEach(item -> {
                joinInSqlBuilder.append("'" + item + "',");
            });
        }
        if("".equals(joinInSqlBuilder.toString().trim())) {
            return null;
        }
        return joinInSqlBuilder.toString().substring(0, joinInSqlBuilder.toString().length() - 1);
    }
    public static String joinLongInSql(List<Long> list) {
        StringBuilder joinInSqlBuilder = new StringBuilder();
        if(list != null && list.size() > 0) {
            list.forEach(item -> {
                joinInSqlBuilder.append("'" + item + "',");
            });
        }
        if("".equals(joinInSqlBuilder.toString().trim())) {
            return null;
        }
        return joinInSqlBuilder.toString().substring(0, joinInSqlBuilder.toString().length() - 1);
    }
}
