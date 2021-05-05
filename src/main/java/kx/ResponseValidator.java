package kx;

import java.util.concurrent.CancellationException;

@FunctionalInterface
public interface ResponseValidator {
    void checkMessageSize(int size) throws CancellationException;
}
