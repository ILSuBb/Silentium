package org.IlSuBb.silentium.data;

import org.IlSuBb.silentium.checks.CheckCategory;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class ViolationEntry {

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final String checkName;
    private final CheckCategory category;
    private final String details;
    private final int vl;
    private final int ping;
    private final LocalDateTime timestamp;

    public ViolationEntry(String checkName, CheckCategory category, String details, int vl, int ping) {
        this.checkName = checkName;
        this.category = category;
        this.details = details;
        this.vl = vl;
        this.ping = ping;
        this.timestamp = LocalDateTime.now();
    }

    public String getCheckName() { return checkName; }
    public CheckCategory getCategory() { return category; }
    public String getDetails() { return details; }
    public int getVl() { return vl; }
    public int getPing() { return ping; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public String getFormattedTime() { return TIME_FMT.format(timestamp); }

    @Override
    public String toString() {
        return "[%s] [%s/%s] VL: %d | Info: %s | Ping: %dms"
                .formatted(getFormattedTime(), category.getDisplayName(), checkName, vl, details, ping);
    }
}
