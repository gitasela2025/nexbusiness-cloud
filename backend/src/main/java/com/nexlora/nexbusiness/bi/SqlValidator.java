package com.nexlora.nexbusiness.bi;

import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Java-side guard. The nex_readonly grants are the real security boundary. */
public final class SqlValidator {

    public static final int MAX_ROWS = 200;

    public static final Set<String> ALLOWED_TABLES =
        Set.of("pos_orders", "products_inventory", "crm_bookings_tickets");

    private static final int CI = Pattern.CASE_INSENSITIVE;

    private static final Pattern STARTS_WITH_SELECT = Pattern.compile("^select\\b", CI);

    private static final Pattern FORBIDDEN = Pattern.compile(
        "\\b(insert|update|delete|drop|alter|create|truncate|grant|revoke|replace|call|exec|execute"
      + "|load|into|outfile|dumpfile|load_file|sleep|benchmark|information_schema|performance_schema"
      + "|mysql|sys|lock|unlock|set|use|prepare|for\\s+update)\\b", CI);

    // EXTRACT(YEAR FROM col) contains FROM but is not a table reference
    private static final Pattern EXTRACT_FROM = Pattern.compile("\\bextract\\s*\\(\\s*\\w+\\s+from\\b", CI);

    private static final Pattern TABLE_REF = Pattern.compile(
        "\\b(?:from|join)\\s+`?([a-z0-9_]+)`?(?:\\s*\\.\\s*`?([a-z0-9_]+)`?)?", CI);

    private static final Pattern COMMA_JOIN = Pattern.compile(
        "\\b(?:from|join)\\s+`?\\w+`?(?:\\.`?\\w+`?)?(?:\\s+(?:as\\s+)?\\w+)?\\s*,", CI);

    private static final Pattern HAS_LIMIT = Pattern.compile("\\blimit\\b", CI);

    private SqlValidator() {}

    /** Returns the safe SQL to run, or throws IllegalArgumentException with a message the LLM can act on. */
    public static String validate(String raw) {
        if (raw == null || raw.isBlank()) throw new IllegalArgumentException("sql is required");

        String sql = raw.trim();
        while (sql.endsWith(";")) sql = sql.substring(0, sql.length() - 1).trim();

        if (sql.contains(";"))
            throw new IllegalArgumentException("Only a single statement is allowed");
        if (sql.contains("--") || sql.contains("/*") || sql.contains("#"))
            throw new IllegalArgumentException("SQL comments are not allowed");
        if (!STARTS_WITH_SELECT.matcher(sql).find())
            throw new IllegalArgumentException("Only SELECT statements are allowed");

        Matcher bad = FORBIDDEN.matcher(sql);
        if (bad.find())
            throw new IllegalArgumentException("Forbidden keyword: " + bad.group(1).toLowerCase());

        String forTables = EXTRACT_FROM.matcher(sql).replaceAll("extract(");

        if (COMMA_JOIN.matcher(forTables).find())
            throw new IllegalArgumentException("Use explicit JOIN instead of comma-separated tables");

        Matcher m = TABLE_REF.matcher(forTables);
        boolean any = false;
        while (m.find()) {
            any = true;
            String table = (m.group(2) != null ? m.group(2) : m.group(1)).toLowerCase();
            if (!ALLOWED_TABLES.contains(table))
                throw new IllegalArgumentException("Table not allowed: " + table
                    + ". Allowed: " + ALLOWED_TABLES);
        }
        if (!any) throw new IllegalArgumentException("Query must read from an allowed table");

        if (!HAS_LIMIT.matcher(sql).find()) sql = sql + " LIMIT " + MAX_ROWS;
        return sql;
    }
}
