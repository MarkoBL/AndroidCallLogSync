package com.markobl.callhistory;

@FunctionalInterface
public interface SyncResultRunner {
    void run(SyncResult syncResult);
}
