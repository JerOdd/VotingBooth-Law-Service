package com.votingbooth.law.core.model.entity;

// Where a law stands in the parliamentary procedure. Derived from the dossier's acts by the job,
// except STALLED, which depends on today's date: never stored, the API derives it from last_act_date.
public enum LawStatus {
    DEPOSITED,
    IN_COMMITTEE,
    IN_PLENARY,
    IN_SENATE,
    CMP,
    NEW_READING,
    ADOPTED,
    CONSTITUTIONAL_COUNCIL,
    PROMULGATED,
    REJECTED,
    WITHDRAWN,
    LAPSED,
    STALLED
}
