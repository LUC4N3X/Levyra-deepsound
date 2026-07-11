package org.schabi.newpipe.extractor.downloader;

import okhttp3.Call;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class CancellableCall {
    private final Call call;
    private final CountDownLatch finished = new CountDownLatch(1);
    private volatile boolean isFinished = false;

    public CancellableCall(Call call) {
        this.call = call;
    }

    public void cancel() {
        call.cancel();
    }

    public boolean isCancelled() {
        return call.isCanceled();
    }

    public boolean isFinished() {
        return isFinished;
    }

    public void setFinished() {
        isFinished = true;
        finished.countDown();
    }

    public boolean await(final long timeout, final TimeUnit unit) throws InterruptedException {
        return finished.await(timeout, unit);
    }

    Call getCall() {
        return call;
    }
}
