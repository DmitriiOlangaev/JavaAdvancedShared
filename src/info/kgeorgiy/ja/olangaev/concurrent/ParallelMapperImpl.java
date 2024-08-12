package info.kgeorgiy.ja.olangaev.concurrent;

import info.kgeorgiy.java.advanced.mapper.ParallelMapper;

import java.util.*;
import java.util.function.Function;
import java.util.stream.IntStream;

public class ParallelMapperImpl implements ParallelMapper {

    private final Queue<Runnable> queue = new ArrayDeque<>();
    private final List<Thread> workers;
    private final Runnable workerRunnable = () -> {
        while (!Thread.currentThread().isInterrupted()) {
            Runnable task;
            synchronized (queue) {
                while (queue.isEmpty()) {
                    try {
                        queue.wait();
                    } catch (InterruptedException ignored) {
                        return;
                    }
                }
                task = queue.poll();
            }
            task.run();
        }
    };

    public ParallelMapperImpl(int threadsCount) {
        if (threadsCount <= 0) {
            throw new IllegalArgumentException("Threads count must be greater than zero");
        }
        workers = IntStream.range(0, threadsCount).mapToObj((it) -> new Thread(workerRunnable)).toList();
        workers.forEach(Thread::start);
    }


    @Override
    public <T, R> List<R> map(Function<? super T, ? extends R> f, List<? extends T> args) throws
            InterruptedException {
        final List<R> result = new ArrayList<>(Collections.nCopies(args.size(), null));
        final List<RuntimeException> resultRuntime = new ArrayList<>(Collections.nCopies(args.size(), null));
        for (int i = 0; i < args.size(); ++i) {
            int ind = i;
            synchronized (queue) {
                queue.add(() -> {
                    try {
                        result.set(ind, f.apply(args.get(ind)));
                    } catch (RuntimeException e) {
                        resultRuntime.set(ind, e);
                    }
                    synchronized (result) {
                        result.notify();
                    }
                });
                queue.notify();
            }
        }
        boolean RuntimeThrown = false;
        for (int i = 0; i < result.size(); ++i) {
            synchronized (result) {
                while (result.get(i) == null) {
                    result.wait();
                }
                if (resultRuntime.get(i) != null) {
                    RuntimeThrown = true;
                }
            }
        }
        if (RuntimeThrown) {
            List<Integer> thrownIndexes = IntStream.range(0, resultRuntime.size())
                    .filter(i -> resultRuntime.get(i) != null)
                    .boxed()
                    .toList();
            RuntimeException exception = new RuntimeException("Get exception in these indexes " + thrownIndexes);
            resultRuntime.stream()
                    .filter(Objects::nonNull)
                    .forEach(exception::addSuppressed);
            throw exception;
        }
        return result;
    }

    @Override
    public void close() {
        workers.forEach(Thread::interrupt);
        workers.forEach((it) -> {
            try {
                it.join();
            } catch (InterruptedException ignored) {
            }
        });
    }
}
