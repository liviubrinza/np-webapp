package com.brinza.notary.workflow;

import com.brinza.notary.domain.Appointment;
import com.brinza.notary.domain.AppointmentStatus;
import com.brinza.notary.domain.Service;
import com.brinza.notary.repository.AppointmentRepository;
import com.brinza.notary.repository.ServiceRepository;
import com.brinza.notary.service.AppointmentManagementService;
import com.brinza.notary.dto.AppointmentListItemView;
import com.brinza.notary.dto.AppointmentListView;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Searching by client name ignores accents in both directions: "Molnar" finds "Molnár", and
 * "Molnár" finds "Molnar".
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@WithMockUser(username = "titi", roles = "TECHNICIAN")
class AppointmentSearchDiacriticsWorkflowTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ServiceRepository serviceRepository;
    @Autowired
    private AppointmentRepository appointmentRepository;
    @Autowired
    private AppointmentManagementService appointmentManagementService;

    private LocalDateTime start;

    @BeforeEach
    void createClients() {
        start = LocalDateTime.now().plusDays(1).withHour(10).withMinute(0).withSecond(0).withNano(0);
        Stream.of("Molnár Katalin", "Molnar Istvan", "Ștefan Ionescu", "Müller Ödön")
                .forEach(this::appointmentFor);
    }

    private void appointmentFor(String clientName) {
        Service service = serviceRepository.findByCode("document-authentication").orElseThrow();
        appointmentRepository.save(new Appointment(clientName, "client@example.com", "0700000000", service,
                start, start.plusMinutes(30), null));
    }

    private List<String> searchNames(String term) {
        AppointmentListView view = appointmentManagementService.searchGrouped(null, null, null, term, null, null);
        return Stream.concat(view.pending().stream(), view.others().stream())
                .map(AppointmentListItemView::clientName)
                .toList();
    }

    @Test
    void plainSearchTermMatchesTheAccentedSpelling() {
        assertThat(searchNames("Molnar")).containsExactlyInAnyOrder("Molnár Katalin", "Molnar Istvan");
    }

    @Test
    void accentedSearchTermMatchesThePlainSpelling() {
        assertThat(searchNames("Molnár")).containsExactlyInAnyOrder("Molnár Katalin", "Molnar Istvan");
    }

    @Test
    void foldingAppliesToOtherLanguagesAndIsCaseInsensitive() {
        assertThat(searchNames("stefan")).containsExactly("Ștefan Ionescu");
        assertThat(searchNames("MULLER")).containsExactly("Müller Ödön");
        assertThat(searchNames("odon")).containsExactly("Müller Ödön");
    }

    @Test
    void searchStillExcludesNonMatchingNames() {
        assertThat(searchNames("Popescu")).isEmpty();
    }

    @Test
    void listPageSearchFindsTheAccentedClient() throws Exception {
        String html = mockMvc.perform(get("/admin/appointments").param("submitted", "1").param("name", "Molnar"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(html).contains("Molnár Katalin").doesNotContain("Müller Ödön");
    }

    @Test
    void storedNormalizedNameFollowsARename() {
        Appointment appointment = appointmentRepository.save(new Appointment("Kovács Béla", "c@example.com",
                "0700000000", serviceRepository.findByCode("document-authentication").orElseThrow(),
                start, start.plusMinutes(30), null));
        assertThat(appointment.getClientNameNormalized()).isEqualTo("kovacs bela");

        appointment.setClientName("Nagy Zsófia");
        appointmentRepository.saveAndFlush(appointment);

        assertThat(searchNames("Nagy Zsofia")).containsExactly("Nagy Zsófia");
        assertThat(searchNames("Kovacs")).isEmpty();
    }

    @Test
    void pendingAndOtherStatusesAreBothSearched() {
        Appointment confirmed = appointmentRepository.findAll().stream()
                .filter(a -> a.getClientName().equals("Molnár Katalin"))
                .findFirst().orElseThrow();
        confirmed.setStatus(AppointmentStatus.CONFIRMED);
        appointmentRepository.saveAndFlush(confirmed);

        assertThat(searchNames("Molnar")).containsExactlyInAnyOrder("Molnár Katalin", "Molnar Istvan");
    }
}
