package com.votingbooth.law.job.model.sync;

// Raw records counted during a run, reported on sync_run.
public class SyncCounts {

    private int created;
    private int updated;
    private int unchanged;
    private int failed;

    public void created() {
        created++;
    }

    public void updated() {
        updated++;
    }

    public void unchanged(int count) {
        unchanged += count;
    }

    public void failed() {
        failed++;
    }

    public int getCreated() {
        return created;
    }

    public int getUpdated() {
        return updated;
    }

    public int getUnchanged() {
        return unchanged;
    }

    public int getFailed() {
        return failed;
    }
}
