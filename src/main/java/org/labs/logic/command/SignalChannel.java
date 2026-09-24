package org.labs.logic.command;

import java.util.concurrent.SynchronousQueue;

public record SignalChannel(
    SynchronousQueue<Signal> input,
    SynchronousQueue<Signal> output
) {
}
