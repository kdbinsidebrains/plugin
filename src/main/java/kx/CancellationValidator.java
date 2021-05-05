package kx;

import java.util.concurrent.CancellationException;

@FunctionalInterface
public interface CancellationValidator {
    void checkCancelled() throws CancellationException;
}
