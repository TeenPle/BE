package com.shu.backend.global.neis;

public record NeisSyncResult(int total, int synced, int ambiguous, int notFound) {}
