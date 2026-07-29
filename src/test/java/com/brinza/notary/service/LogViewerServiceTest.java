package com.brinza.notary.service;

import com.brinza.notary.dto.AdminActivityEntryView;
import com.brinza.notary.dto.LogEntryView;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class LogViewerServiceTest {

    @TempDir
    Path tempDir;

    private LogViewerService service() {
        return new LogViewerService(tempDir.toString());
    }

    @Test
    void availableDatesIncludesActiveFileAsToday() throws IOException {
        Files.writeString(tempDir.resolve("log"), "2026-07-29 10:00:00.000: [] [Foo] INFO: hi\n");

        List<LocalDate> dates = service().availableDates();

        assertThat(dates).contains(LocalDate.now());
    }

    @Test
    void availableDatesParsesRolledFilenamesAndSkipsUnparseable() throws IOException {
        LocalDate rolledDate = LocalDate.now().minusDays(1);
        String rolledName = "log_" + rolledDate.format(DateTimeFormatter.ofPattern("dd_MMM_yyyy"));
        Files.writeString(tempDir.resolve(rolledName), "2026-07-28 10:00:00.000: [] [Foo] INFO: hi\n");
        Files.writeString(tempDir.resolve("log_not_a_date"), "irrelevant\n");

        List<LocalDate> dates = service().availableDates();

        assertThat(dates).containsExactly(rolledDate);
    }

    @Test
    void readEntriesParsesMultilineContinuation() throws IOException {
        String content = """
                2026-07-29 10:00:00.000: [corr-1] [SomeClass] ERROR: boom
                \tat com.example.Foo.bar(Foo.java:10)
                \tat com.example.Foo.baz(Foo.java:20)
                2026-07-29 10:00:01.000: [corr-2] [OtherClass] INFO: next entry
                """;
        Files.writeString(tempDir.resolve("log"), content);

        List<LogEntryView> entries = service().readEntries(LocalDate.now(), null, null, null, null);

        assertThat(entries).hasSize(2);
        assertThat(entries.get(0).message()).contains("boom").contains("at com.example.Foo.bar");
        assertThat(entries.get(1).correlationId()).isEqualTo("corr-2");
    }

    @Test
    void readEntriesFiltersByLevelCorrelationClassAndText() throws IOException {
        String content = """
                2026-07-29 10:00:00.000: [corr-1] [ClassA] INFO: first message
                2026-07-29 10:00:01.000: [corr-2] [ClassB] ERROR: second message
                """;
        Files.writeString(tempDir.resolve("log"), content);
        LogViewerService service = service();

        assertThat(service.readEntries(LocalDate.now(), null, "ERROR", null, null)).hasSize(1);
        assertThat(service.readEntries(LocalDate.now(), "corr-1", null, null, null)).hasSize(1);
        assertThat(service.readEntries(LocalDate.now(), null, null, "ClassB", null)).hasSize(1);
        assertThat(service.readEntries(LocalDate.now(), null, null, null, "second")).hasSize(1);
        assertThat(service.readEntries(LocalDate.now(), null, null, null, "nomatch")).isEmpty();
    }

    @Test
    void readEntriesReturnsEmptyWhenNoFileForDate() {
        assertThat(service().readEntries(LocalDate.now().minusDays(5), null, null, null, null)).isEmpty();
    }

    @Test
    void readActivityEntriesOnlyIncludesAdminActivityLoggerLines() throws IOException {
        String content = """
                2026-07-29 10:00:00.000: [corr-1] [AdminActivity] INFO: [titi] Did a thing
                2026-07-29 10:00:01.000: [corr-2] [SomeOtherClass] INFO: not activity
                """;
        Files.writeString(tempDir.resolve("log"), content);

        List<AdminActivityEntryView> activity = service().readActivityEntries(LocalDate.now(), null, null, null, false);

        assertThat(activity).hasSize(1);
        assertThat(activity.get(0).username()).isEqualTo("titi");
        assertThat(activity.get(0).action()).isEqualTo("Did a thing");
    }

    @Test
    void readActivityEntriesFiltersByUsername() throws IOException {
        String content = """
                2026-07-29 10:00:00.000: [corr-1] [AdminActivity] INFO: [titi] Action one
                2026-07-29 10:00:01.000: [corr-2] [AdminActivity] INFO: [admin] Action two
                """;
        Files.writeString(tempDir.resolve("log"), content);

        List<AdminActivityEntryView> activity = service().readActivityEntries(LocalDate.now(), "admin", null, null, false);

        assertThat(activity).hasSize(1);
        assertThat(activity.get(0).username()).isEqualTo("admin");
    }

    @Test
    void readActivityEntriesSortsByUsernameWhenRequested() throws IOException {
        String content = """
                2026-07-29 10:00:00.000: [corr-1] [AdminActivity] INFO: [zeta] Action one
                2026-07-29 10:00:01.000: [corr-2] [AdminActivity] INFO: [alpha] Action two
                """;
        Files.writeString(tempDir.resolve("log"), content);

        List<AdminActivityEntryView> sorted = service().readActivityEntries(LocalDate.now(), null, null, null, true);

        assertThat(sorted).extracting(AdminActivityEntryView::username).containsExactly("alpha", "zeta");
    }
}
