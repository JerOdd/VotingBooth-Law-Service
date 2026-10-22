package com.votingbooth.law.api.service;

import com.votingbooth.law.api.model.dto.LawDto;
import com.votingbooth.law.api.model.dto.LawsResponse;
import com.votingbooth.law.core.dataaccess.repository.LawRepository;
import com.votingbooth.law.core.model.entity.Law;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class LawServiceTests {

    private LawRepository lawRepository;
    private LawService lawService;

    @BeforeEach
    void setUp() {
        lawRepository = mock(LawRepository.class);
        lawService = new LawService(lawRepository);
    }

    @Test
    void getLaws() {
        UUID lawId = UUID.randomUUID();
        when(lawRepository.findAll()).thenReturn(List.of(new Law(lawId)));

        LawsResponse response = lawService.getLaws();

        assertEquals(List.of(new LawDto(lawId)), response.laws());
    }

    @Test
    void noLaws() {
        when(lawRepository.findAll()).thenReturn(List.of());

        assertTrue(lawService.getLaws().laws().isEmpty());
    }

}
