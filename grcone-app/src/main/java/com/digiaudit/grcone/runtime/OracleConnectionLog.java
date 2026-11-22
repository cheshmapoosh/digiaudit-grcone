package com.digiaudit.grcone.runtime;

import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.core.env.Environment;
import org.springframework.util.StringUtils;

public final class OracleConnectionLog {

    private static final Logger log = LoggerFactory.getLogger(OracleConnectionLog.class);
    private static final String ORACLE_THIN_PREFIX = "jdbc:oracle:thin:@";
    private static final String HOST = "(?:\\[[0-9a-f:.]+]|[a-z0-9_.-]+)(?::[0-9]+)?";
    private static final Pattern EASY_CONNECT = Pattern.compile(
            "(?:tcps?://|//)?" + HOST + "(?:," + HOST + ")*(?:[/:][a-z0-9_.-]+)?",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern DESCRIPTOR_FIELD = Pattern.compile(
            "\\(\\s*(HOST|PORT|SERVICE_NAME|SID)\\s*=\\s*([a-z0-9_.:-]+)\\s*\\)",
            Pattern.CASE_INSENSITIVE);

    private OracleConnectionLog() {
    }

    public static void logStartupTarget(Environment environment) {
        Binder binder = Binder.get(environment);
        String hikariUrl = binder.bind("spring.datasource.hikari.jdbc-url", String.class).orElse("");
        String datasourceUrl = StringUtils.hasText(hikariUrl)
                ? hikariUrl : binder.bind("spring.datasource.url", String.class).orElse("");
        log.info("Oracle target: {} (Spring datasource)", describe(datasourceUrl));

        boolean flywayEnabled = binder.bind("spring.flyway.enabled", Boolean.class).orElse(true);
        String flywayUrl = binder.bind("spring.flyway.url", String.class).orElse("");
        if (flywayEnabled && StringUtils.hasText(flywayUrl) && !flywayUrl.equals(datasourceUrl)) {
            log.info("Oracle target: {} (Flyway)", describe(flywayUrl));
        }
    }

    static String describe(String jdbcUrl) {
        if (!jdbcUrl.startsWith(ORACLE_THIN_PREFIX)) {
            return "unavailable for this JDBC URL format";
        }
        String target = jdbcUrl.substring(ORACLE_THIN_PREFIX.length()).split("[?;]", 2)[0];
        if (EASY_CONNECT.matcher(target).matches()) {
            return target;
        }
        if (target.startsWith("(")) {
            String fields = DESCRIPTOR_FIELD.matcher(target).results()
                    .map(field -> "(" + field.group(1) + "=" + field.group(2) + ")")
                    .collect(Collectors.joining(" "));
            if (!fields.isEmpty()) {
                return fields;
            }
        }
        return "unavailable for this JDBC URL format";
    }
}
