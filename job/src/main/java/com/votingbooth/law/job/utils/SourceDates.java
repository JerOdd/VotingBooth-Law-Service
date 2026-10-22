package com.votingbooth.law.job.utils;

import java.time.LocalDate;

// Source dates come as 2025-02-17T00:00:00.000+01:00 or 2025-02-18+01:00, in French local time: the first ten characters are the date.
public final class SourceDates {

    private SourceDates() {
    }

    public static LocalDate parse(String value) {
        return value == null || value.length() < 10 ? null : LocalDate.parse(value.substring(0, 10));
    }
}
