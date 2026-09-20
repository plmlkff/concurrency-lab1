package org.labs.domain;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class Spoon {
    private final Lock lock;

    public Spoon() {
        this(new ReentrantLock());
    }

    public Spoon(Lock lock) {
        this.lock = lock;
    }

    public void take() {
        lock.lock();
    }

    public void put() {
        lock.unlock();
    }
}
