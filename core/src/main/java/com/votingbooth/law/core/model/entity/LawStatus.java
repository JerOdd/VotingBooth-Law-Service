package com.votingbooth.law.core.model.entity;

// Where a law stands in the parliamentary procedure. Derived from the dossier's acts by the job.
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
    LAPSED
}
