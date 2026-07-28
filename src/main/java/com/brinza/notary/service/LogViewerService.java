package com.brinza.notary.service;

import com.brinza.notary.dto.AdminActivityEntryView;
import com.brinza.notary.dto.LogEntryView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Lists and reads the rotating application log files written by {@code logback-spring.xml}
 * (active file named "log", rolled files named "log_dd_MMM_yyyy") for the admin Logs panel.
 * The rollover date suffix is rendered by Logback using the JVM's default locale, so the
 * {@link #ROLLED_FILE_DATE} formatter here intentionally uses the default locale too rather
 * than a hardcoded one, to stay in sync with whatever Logback actually wrote.
 */
@org.springframework.stereotype.Service
public class LogViewerService {

    private static final Logger log = LoggerFactory.getLogger(LogViewerService.class);

    private static final String ACTIVE_FILENAME = "log";
    private static final Pattern ROLLED_FILENAME = Pattern.compile("^log_(\\d{2}_[A-Za-z]{3}_\\d{4})$");
    private static final DateTimeFormatter ROLLED_FILE_DATE = DateTimeFormatter.ofPattern("dd_MMM_yyyy");

    private static final Pattern LOG_LINE = Pattern.compile(
            "^(\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}\\.\\d{3}): \\[([^\\]]*)\\] \\[([^\\]]*)\\] (\\S+): (.*)$");

    /** The logger name {@link AdminActivityLogger} writes to; identifies which parsed entries are admin activity. */
    private static final String ADMIN_ACTIVITY_LOGGER = "AdminActivity";
    /** Matches the "[username] action text" convention {@link AdminActivityLogger} writes into its messages. */
    private static final Pattern ACTIVITY_MESSAGE = Pattern.compile("^\\[([^\\]]*)\\] (.*)$");

    private final Path logDir;

    public LogViewerService(@Value("${app.logging.dir}") String logDir) {
        this.logDir = Path.of(logDir).toAbsolutePath().normalize();
    }

    public List<LocalDate> availableDates() {
        log.info("availableDates called");
        List<LocalDate> dates = new ArrayList<>(logFilesByDate().keySet());
        dates.sort(Comparator.reverseOrder());
        log.debug("Found {} available log date(s)", dates.size());
        return dates;
    }

    public List<LogEntryView> readEntries(LocalDate date, String correlationId, String level, String className, String text) {
        log.info("readEntries called for date={} correlationId={} level={} className={} text={}", date, correlationId, level, className, text);
        Path file = logFilesByDate().get(date);
        if (file == null) {
            log.debug("No log file found for date={}", date);
            return List.of();
        }

        List<LogEntryView> entries = parse(file);
        log.debug("Parsed {} log entr(ies) from {}", entries.size(), file.getFileName());
        return filter(entries, correlationId, level, className, text);
    }

    /**
     * Reads only the curated admin/technician action trail written via {@link AdminActivityLogger}
     * for the given day - not the full firehose of every logged method call - optionally sorted
     * by username instead of chronologically.
     */
    public List<AdminActivityEntryView> readActivityEntries(LocalDate date, String username, String correlationId,
                                                              String text, boolean sortByUsername) {
        log.info("readActivityEntries called for date={} username={} correlationId={} text={} sortByUsername={}",
                date, username, correlationId, text, sortByUsername);
        Path file = logFilesByDate().get(date);
        if (file == null) {
            log.debug("No log file found for date={}", date);
            return List.of();
        }

        List<AdminActivityEntryView> activity = parse(file).stream()
                .filter(e -> ADMIN_ACTIVITY_LOGGER.equals(e.className()))
                .map(LogViewerService::toActivityView)
                .toList();
        log.debug("Found {} admin activity entr(ies) in {}", activity.size(), file.getFileName());

        activity = filterActivity(activity, username, correlationId, text);
        if (sortByUsername) {
            activity = activity.stream()
                    .sorted(Comparator.comparing(AdminActivityEntryView::username, String.CASE_INSENSITIVE_ORDER)
                            .thenComparing(AdminActivityEntryView::timestamp))
                    .toList();
        }
        return activity;
    }

    private static AdminActivityEntryView toActivityView(LogEntryView entry) {
        Matcher m = ACTIVITY_MESSAGE.matcher(entry.message());
        if (m.matches()) {
            return new AdminActivityEntryView(entry.timestamp(), entry.correlationId(), m.group(1), m.group(2));
        }
        return new AdminActivityEntryView(entry.timestamp(), entry.correlationId(), "", entry.message());
    }

    private static List<AdminActivityEntryView> filterActivity(List<AdminActivityEntryView> activity, String username,
                                                                 String correlationId, String text) {
        String usernameFilter = normalize(username);
        String correlationFilter = normalize(correlationId);
        String textFilter = normalize(text);

        return activity.stream()
                .filter(a -> usernameFilter == null || a.username().toLowerCase(Locale.ROOT).contains(usernameFilter))
                .filter(a -> correlationFilter == null || a.correlationId().toLowerCase(Locale.ROOT).contains(correlationFilter))
                .filter(a -> textFilter == null || matchesActivityText(a, textFilter))
                .toList();
    }

    private static boolean matchesActivityText(AdminActivityEntryView entry, String textFilter) {
        return containsIgnoreCase(entry.timestamp(), textFilter)
                || containsIgnoreCase(entry.correlationId(), textFilter)
                || containsIgnoreCase(entry.username(), textFilter)
                || containsIgnoreCase(entry.action(), textFilter);
    }

    private Map<LocalDate, Path> logFilesByDate() {
        if (!Files.isDirectory(logDir)) {
            log.debug("Log directory does not exist: {}", logDir);
            return Map.of();
        }
        Map<LocalDate, Path> byDate = new TreeMap<>();
        try (var listing = Files.list(logDir)) {
            for (Path file : (Iterable<Path>) listing::iterator) {
                String filename = file.getFileName().toString();
                if (filename.equals(ACTIVE_FILENAME)) {
                    byDate.put(LocalDate.now(), file);
                    continue;
                }
                Matcher m = ROLLED_FILENAME.matcher(filename);
                if (m.matches()) {
                    try {
                        byDate.put(LocalDate.parse(m.group(1), ROLLED_FILE_DATE), file);
                    } catch (DateTimeParseException e) {
                        log.debug("Skipping unparseable log filename={}", filename);
                    }
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to list log directory " + logDir, e);
        }
        return byDate;
    }

    private static List<LogEntryView> parse(Path file) {
        List<String> lines;
        try {
            lines = Files.readAllLines(file);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read log file " + file, e);
        }

        List<LogEntryView> entries = new ArrayList<>();
        String timestamp = null, correlationId = null, className = null, level = null;
        StringBuilder message = null;

        for (String line : lines) {
            Matcher m = LOG_LINE.matcher(line);
            if (m.matches()) {
                if (message != null) {
                    entries.add(new LogEntryView(timestamp, correlationId, className, level, message.toString()));
                }
                timestamp = m.group(1);
                correlationId = m.group(2);
                className = m.group(3);
                level = m.group(4);
                message = new StringBuilder(m.group(5));
            } else if (message != null) {
                // Continuation line (e.g. a stack trace), belongs to the entry currently being built.
                message.append('\n').append(line);
            }
        }
        if (message != null) {
            entries.add(new LogEntryView(timestamp, correlationId, className, level, message.toString()));
        }
        return entries;
    }

    private static List<LogEntryView> filter(List<LogEntryView> entries, String correlationId, String level, String className, String text) {
        String correlationFilter = normalize(correlationId);
        String classFilter = normalize(className);
        String levelFilter = level != null && !level.isBlank() ? level.trim().toUpperCase(Locale.ROOT) : null;
        String textFilter = normalize(text);

        return entries.stream()
                .filter(e -> correlationFilter == null || e.correlationId().toLowerCase(Locale.ROOT).contains(correlationFilter))
                .filter(e -> classFilter == null || e.className().toLowerCase(Locale.ROOT).contains(classFilter))
                .filter(e -> levelFilter == null || e.level().equalsIgnoreCase(levelFilter))
                .filter(e -> textFilter == null || matchesText(e, textFilter))
                .toList();
    }

    /**
     * "Contains this text anywhere in the entry" - checked across every field, not just the
     * message, so a technician can search by e.g. a timestamp fragment too without needing to
     * know which field it's in.
     */
    private static boolean matchesText(LogEntryView entry, String textFilter) {
        return containsIgnoreCase(entry.timestamp(), textFilter)
                || containsIgnoreCase(entry.correlationId(), textFilter)
                || containsIgnoreCase(entry.className(), textFilter)
                || containsIgnoreCase(entry.level(), textFilter)
                || containsIgnoreCase(entry.message(), textFilter);
    }

    private static boolean containsIgnoreCase(String value, String textFilter) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(textFilter);
    }

    private static String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }
}
