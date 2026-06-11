package org.IlSuBb.silentium.managers;

import org.IlSuBb.silentium.Silentium;
import org.IlSuBb.silentium.checks.CheckBase;
import org.IlSuBb.silentium.data.PlayerData;
import org.IlSuBb.silentium.utils.ColorUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.io.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class NotificationManager {

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final Silentium plugin;
    private PrintWriter logWriter;
    private LocalDate currentLogDate;

    public NotificationManager(Silentium plugin) {
        this.plugin = plugin;
        openLogFile();
    }

    // ── Alert broadcasting ─────────────────────────────────────────────────

    public void sendAlert(Player suspect, CheckBase check, String details, int vl) {
        String raw = plugin.getConfigManager().getNotificationFormat();
        raw = ColorUtils.replace(raw,
                "player",   suspect.getName(),
                "check",    check.getName(),
                "vl",       String.valueOf(vl),
                "category", check.getCategory().getDisplayName(),
                "info",     details);

        var component = ColorUtils.parse(raw);

        // Broadcast to staff
        if (plugin.getConfigManager().isBroadcastToStaff()) {
            for (Player staff : Bukkit.getOnlinePlayers()) {
                if (hasAlertPermission(staff)) {
                    PlayerData sd = plugin.getPlayerDataManager().get(staff.getUniqueId());
                    if (sd == null || sd.isAlertsEnabled()) {
                        staff.sendMessage(component);
                    }
                    // Verbose suffix for verbose-watching staff
                    if (sd != null && sd.isAlertsEnabled()) {
                        PlayerData pd = plugin.getPlayerDataManager().get(suspect.getUniqueId());
                        if (pd != null && pd.isVerboseTarget()) {
                            staff.sendMessage(ColorUtils.parse(
                                    "<dark_gray>  └ Verbose: " + details
                                    + " | Ping: " + suspect.getPing() + "ms"));
                        }
                    }
                }
            }
        }

        // Console
        if (plugin.getConfigManager().isDebug() || plugin.getConfigManager().isLogConsoleDebug()) {
            plugin.getLogger().info(ColorUtils.stripTags(raw));
        }

        // File log
        writeLog("[%s] [%s/%s] Player: %s | VL: %d | Info: %s | Ping: %dms".formatted(
                TIME_FMT.format(LocalDateTime.now()),
                check.getCategory().getDisplayName(),
                check.getName(),
                suspect.getName(),
                vl, details,
                suspect.getPing()));
    }

    private boolean hasAlertPermission(Player p) {
        return p.hasPermission("silentium.alerts")
                || p.hasPermission("silentium.mod")
                || p.hasPermission("silentium.admin");
    }

    // ── File logging ───────────────────────────────────────────────────────

    private void openLogFile() {
        if (!plugin.getConfigManager().isLogFile()) return;
        try {
            currentLogDate = LocalDate.now();
            File logDir = new File(plugin.getDataFolder(), "logs");
            logDir.mkdirs();
            File file = new File(logDir, DATE_FMT.format(currentLogDate) + ".log");
            logWriter = new PrintWriter(new FileWriter(file, true));
        } catch (IOException e) {
            plugin.getLogger().warning("Could not open log file: " + e.getMessage());
        }
    }

    private void writeLog(String line) {
        if (!plugin.getConfigManager().isLogFile() || logWriter == null) return;
        // Rotate if day changed
        if (!LocalDate.now().equals(currentLogDate)) {
            closeLogFile();
            openLogFile();
        }
        logWriter.println(line);
        logWriter.flush();
    }

    public void closeLogFile() {
        if (logWriter != null) {
            logWriter.close();
            logWriter = null;
        }
    }

    /** Returns the last {@code count} lines from today's log file. */
    public List<String> getRecentLogs(int count) {
        File logFile = new File(plugin.getDataFolder(),
                "logs/" + DATE_FMT.format(LocalDate.now()) + ".log");
        List<String> lines = new ArrayList<>();
        if (!logFile.exists()) return lines;
        try (var reader = new BufferedReader(new FileReader(logFile))) {
            String line;
            while ((line = reader.readLine()) != null) lines.add(line);
        } catch (IOException e) {
            plugin.getLogger().warning("Could not read log file: " + e.getMessage());
        }
        int start = Math.max(0, lines.size() - count);
        return lines.subList(start, lines.size());
    }
}
