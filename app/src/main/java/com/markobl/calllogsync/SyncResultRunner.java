package com.markobl.calllogsync;

@FunctionalInterface
public interface SyncResultRunner {
    void run(SyncResult syncResult);
}
