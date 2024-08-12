package info.kgeorgiy.ja.olangaev.myutils;

import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

/**
 * A collection of utility methods for common tasks.
 */
public class MyUtilities {

    /**
     * Retrieves the argument at a given index from an array of arguments and parses it with a specified function.
     * If the index is out of range, returns a default value instead.
     *
     * @param args         the array of arguments
     * @param ind          the index of the argument to retrieve
     * @param parser       the function used to parse the argument
     * @param defaultValue the value returned if the index is out of range
     * @param <T>          the type of the parsed argument
     * @return the parsed argument or the default value if the index is out of range
     * @throws IllegalArgumentException if the argument cannot be parsed by the specified function
     */
    public static <T> T getArgument(String[] args, int ind, Function<? super String, ? extends T> parser, T defaultValue) {
        if(args == null) {
            System.err.println("Args must be not null");
            throw new IllegalArgumentException("Got null args argument");
        }
        try {
            return ind < args.length || defaultValue == null ? parser.apply(args[ind]) : defaultValue;
        } catch (IllegalArgumentException e) {
            System.err.println("Incorrect argument at pos " + ind + System.lineSeparator() + "You gave " + Arrays.toString(args));
            System.err.println(e.getMessage());
            throw e;
        }
    }

    /**
     * Verifies that a value is within a specified range.
     * Throws an IllegalArgumentException if the value is outside the range.
     *
     * @param name       the name of the value being checked
     * @param value      the value to check
     * @param leftBound  the lower bound of the range (inclusive)
     * @param rightBound the upper bound of the range (inclusive)
     * @param <T>        the type of the value being checked, which must implement Comparable
     * @throws IllegalArgumentException if the value is not within the specified range
     */
    public static <T extends Comparable<T>> void checkBounds(String name, T value, T leftBound, T rightBound) {
        if (value.compareTo(leftBound) < 0 || value.compareTo(rightBound) > 0) {
            throw new IllegalArgumentException(name + " isn't between " + leftBound + " and " + rightBound);
        }
    }

    /**
     * Waits for the termination of one or more ExecutorServices.
     *
     * @param es the ExecutorServices to wait for
     */
    public static void await(ExecutorService... es) {
        boolean isInterrupted = false;
        for (ExecutorService e : es) {
            while (true) {
                try {
                    if (e.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS)) {
                        break;
                    }
                } catch (InterruptedException ignored) {
                    isInterrupted = isInterrupted || Thread.interrupted();
                }
            }
        }
        if (isInterrupted) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Shuts down multiple ExecutorServices immediately and waits for their termination.
     *
     * @param es the ExecutorServices to shut down
     */
    public static void shutdownNowAndAwait(ExecutorService... es) {
        Arrays.stream(es).forEach(ExecutorService::shutdownNow);
        await(es);
    }

    /**
     * Shuts down multiple ExecutorServices and waits for their termination.
     *
     * @param es the ExecutorServices to shut down
     */
    public static void shutdownAndAwait(ExecutorService... es) {
        Arrays.stream(es).forEach(ExecutorService::shutdown);
        await(es);
    }
}
