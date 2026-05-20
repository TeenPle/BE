package com.shu.backend.global.neis;

public record NeisSyncResult(
        int total,
        int synced,
        int created,
        int updated,
        int unchanged,
        int skipped,
        int ambiguous,
        int notFound,
        boolean dryRun
) {
    public static NeisSyncResult legacy(int total, int synced, int ambiguous, int notFound) {
        return new NeisSyncResult(total, synced, 0, synced, 0, 0, ambiguous, notFound, false);
    }
}
