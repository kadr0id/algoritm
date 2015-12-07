/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package scripting.util.concurrent;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 *
 * @author GOD
 */
public class ManualResetEvent {

    private volatile CountDownLatch event;
    private final Integer mutex;
    private String source = "";

    public void setSource(String source) {
        synchronized (mutex) {
            if (this.source.equals("cancelled")) {
                return;
            } else {
                this.source = source;
            }
        }
    }

    public String getSource() {
        return this.source;
    }

    public ManualResetEvent(boolean signalled) {
        this.source = "";
        mutex = new Integer(-1);
        if (signalled) {
            event = new CountDownLatch(0);
        } else {
            event = new CountDownLatch(1);
        }
    }

    public void set() {
        event.countDown();
    }

    public void reset() {
        synchronized (mutex) {
            if (this.source.equals("cancelled")) {
                return;
            }
            this.source = "";
            if (event.getCount() == 0) {
                event = new CountDownLatch(1);
            }
        }
    }

    public void waitOne() throws InterruptedException {
        event.await();
    }

    public boolean waitOne(int timeout, TimeUnit unit) throws InterruptedException {
        if (timeout == 0) {
            if (unit.equals(TimeUnit.SECONDS)) {
                timeout = 10;
            } else {
                timeout = 10 * 1000;
            }
        }
        return event.await(timeout, unit);
    }

    public boolean isSignalled() {
        return event.getCount() == 0;
    }
}