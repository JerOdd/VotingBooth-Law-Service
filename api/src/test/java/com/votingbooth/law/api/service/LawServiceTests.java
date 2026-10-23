package com.votingbooth.law.api.service;

import com.votingbooth.law.api.model.dto.LawSummaryDto;
import com.votingbooth.law.api.model.dto.LawsResponse;
import com.votingbooth.law.api.model.exception.InvalidPageException;
import com.votingbooth.law.core.dataaccess.repository.LawRepository;
import com.votingbooth.law.core.model.entity.InitiatorType;
import com.votingbooth.law.core.model.entity.LawStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class LawServiceTests {

    // 2026-10-22 00:30 in Paris, still 2026-10-21 in UTC
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-21T22:30:00Z"), ZoneOffset.UTC);

    private LawRepository lawRepository;
    private LawService lawService;

    @BeforeEach
    void setUp() {
        lawRepository = mock(LawRepository.class);
        lawService = new LawService(lawRepository, CLOCK, 6);
    }

    @Test
    void getLaws() {
        UUID lawId = UUID.randomUUID();
        LawRepository.LawSummary summary = summary(lawId, "BUDGET,ECONOMY");
        when(lawRepository.findSummaries(any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(summary), PageRequest.of(1, 20), 41));

        LawsResponse response = lawService.getLaws(1, 20, null, null);

        LawSummaryDto law = response.laws().getFirst();
        assertEquals(lawId, law.id());
        assertEquals("DLR5L17N51852", law.externalId());
        assertEquals("Simplification du droit de l'urbanisme et du logement", law.shortTitle());
        assertEquals(LawStatus.PROMULGATED, law.status());
        assertEquals(InitiatorType.DEPUTY, law.initiatorType());
        assertEquals(List.of("BUDGET", "ECONOMY"), law.categories());
        assertEquals(1, response.page());
        assertEquals(41, response.totalElements());
        assertEquals(3, response.totalPages());
        // Stalled before six months ago, counted from the Paris date
        verify(lawRepository).findSummaries(null, null, LocalDate.of(2026, 4, 22), PageRequest.of(1, 20));
    }

    @Test
    void lawWithoutCategory() {
        LawRepository.LawSummary summary = summary(UUID.randomUUID(), null);
        when(lawRepository.findSummaries(any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(summary), PageRequest.of(0, 20), 1));

        assertEquals(List.of(), lawService.getLaws(0, 20, null, null).laws().getFirst().categories());
    }

    @Test
    void filters() {
        when(lawRepository.findSummaries(any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 10), 0));

        LawsResponse response = lawService.getLaws(0, 10, LawStatus.STALLED, "DEFENSE");

        assertTrue(response.laws().isEmpty());
        verify(lawRepository).findSummaries(eq("STALLED"), eq("DEFENSE"), any(), eq(PageRequest.of(0, 10)));
    }

    @Test
    void invalidPage() {
        assertThrows(InvalidPageException.class, () -> lawService.getLaws(-1, 20, null, null));
        assertThrows(InvalidPageException.class, () -> lawService.getLaws(0, 0, null, null));
        assertThrows(InvalidPageException.class, () -> lawService.getLaws(0, 101, null, null));
        verifyNoInteractions(lawRepository);
    }

    private static LawRepository.LawSummary summary(UUID id, String categories) {
        LawRepository.LawSummary summary = mock(LawRepository.LawSummary.class);
        when(summary.getId()).thenReturn(id);
        when(summary.getExternalId()).thenReturn("DLR5L17N51852");
        when(summary.getTitle()).thenReturn("Simplification du droit de l'urbanisme et du logement");
        when(summary.getShortTitle()).thenReturn("Simplification du droit de l'urbanisme et du logement");
        when(summary.getLegislature()).thenReturn(17);
        when(summary.getInitiatorType()).thenReturn("DEPUTY");
        when(summary.getStatus()).thenReturn("PROMULGATED");
        when(summary.getCategories()).thenReturn(categories);
        return summary;
    }

}
