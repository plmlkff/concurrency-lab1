package org.labs.domain;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class Spoon implements Comparable<Spoon> {
    private final Lock lock;
    private final int order;

    public Spoon(int order) {
        this(new ReentrantLock(), order);
    }

    public Spoon(Lock lock, int order) {
        this.lock = lock;
        this.order = order;
    }

    public void take() {
        lock.lock();
    }

    public void put() {
        lock.unlock();
    }

    public int getOrder() {
        return order;
    }

    @Override
    public int compareTo(Spoon o) {
        return order - o.order;
    }
}
