package org.python.pydev.core.concurrency;

import org.python.pydev.shared_core.callbacks.ICallback0;

public class ConditionEventWithValue<X> {

    private final Object lock = new Object();
    private final ICallback0<X> markSetIfConditionReturnedNonNull;
    private final long timeout;

    private X set = null;

    /**
     * @param timeout: only used if markSetIfConditionReturnedNonNull != null
     */
    public ConditionEventWithValue(ICallback0<X> markSetIfConditionReturnedNonNull, long timeout) {
        this.markSetIfConditionReturnedNonNull = markSetIfConditionReturnedNonNull;
        this.timeout = timeout;
    }

    public void set(X v) {
        synchronized (lock) {
            set = v;
            lock.notifyAll();
        }
    }

    public void unset() {
        synchronized (lock) {
            set = null;
        }
    }

    public X waitForSet() {
        if (markSetIfConditionReturnedNonNull != null) {

            synchronized (lock) {
                while (set == null) {
                    try {
                        lock.wait(timeout);
                    } catch (InterruptedException e) {
                        //ok
                    }
                    set = this.markSetIfConditionReturnedNonNull.call();
                }
                return set;
            }
        } else {
            synchronized (lock) {
                while (set == null) {
                    try {
                        lock.wait(); //no timeout
                    } catch (InterruptedException e) {
                        //ok
                    }
                }
                return set;
            }
        }
    }

    public X get() {
        synchronized (lock) {
            return set;
        }
    }

}
