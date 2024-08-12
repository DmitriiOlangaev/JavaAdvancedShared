package info.kgeorgiy.ja.olangaev.concurrent;

import info.kgeorgiy.java.advanced.concurrent.ListIP;
import info.kgeorgiy.java.advanced.mapper.ParallelMapper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.function.*;

public class IterativeParallelism implements ListIP {
    private final ParallelMapper parallelMapper;


    public IterativeParallelism() {
        parallelMapper = null;
    }

    public IterativeParallelism(ParallelMapper parallelMapper) {
        this.parallelMapper = parallelMapper;
    }

    @Override
    public String join(int threads, List<?> values) throws InterruptedException {
        return parallelWork(threads, values, Object::toString, StringBuilder::new, (sb, e) -> {
            sb.append(e);
            return sb;
        }, (sb1, sb2) -> {
            sb1.append(sb2);
            return sb1;
        }).toString();
    }

    @Override
    public <T> List<T> filter(int threads, List<? extends T> values, Predicate<? super T> predicate) throws InterruptedException {
        return parallelWork(threads, values, Function.identity(), ArrayList::new, (l, e) -> {
            if (predicate.test(e)) {
                l.add(e);
            }
            return l;
        }, (l1, l2) -> {
            l1.addAll(l2);
            return l1;
        });
    }

    @Override
    public <T, U> List<U> map(int threads, List<? extends T> values, Function<? super T, ? extends U> f) throws InterruptedException {
        return parallelWork(threads, values, f, ArrayList::new, (l, e) -> {
            l.add(e);
            return l;
        }, (l1, l2) -> {
            l1.addAll(l2);
            return l1;
        });
    }


    @Override
    public <T> T maximum(int threads, List<? extends T> values, Comparator<? super T> comparator) throws InterruptedException {
        return minimum(threads, values, comparator.reversed());
    }

    @Override
    public <T> T minimum(int threads, List<? extends T> values, Comparator<? super T> comparator) throws InterruptedException {
        BinaryOperator<T> f = (x, y) -> comparator.compare(x, y) <= 0 ? x : y;
        return parallelWork(threads, values, Function.identity(), () -> values.isEmpty() ? null : values.get(0), f, f);
    }

    @Override
    public <T> boolean all(int threads, List<? extends T> values, Predicate<? super T> predicate) throws InterruptedException {
        return !any(threads, values, predicate.negate());
    }

    @Override
    public <T> boolean any(int threads, List<? extends T> values, Predicate<? super T> predicate) throws InterruptedException {
        return parallelWork(threads, values, predicate::test, () -> false, Boolean::logicalOr, Boolean::logicalOr);
    }

    @Override
    public <T> int count(int threads, List<? extends T> values, Predicate<? super T> predicate) throws InterruptedException {
        return parallelWork(threads, values, (it) -> predicate.test(it) ? 1 : 0, () -> 0, Integer::sum, Integer::sum);
    }

    private <T> List<T> createListIf(boolean need, int size) {
        return need ? new ArrayList<>(Collections.nCopies(size, null)) : null;
    }

    private <S, R, T> S parallelWork(int threads, List<? extends T> values, Function<? super T, ? extends R> mapper, Supplier<S> zeroSupplier,
                                     BiFunction<S, ? super R, S> accumulator,
                                     BinaryOperator<S> combiner) throws InterruptedException {
        if (threads <= 0) {
            throw new IllegalArgumentException("Not positive threads count");
        }
        if (values.isEmpty()) {
            return zeroSupplier.get();
        }
        threads = Math.min(threads, values.size());
        final List<Thread> threadsList = createListIf(parallelMapper == null, threads);
        final List<S> threadsResults = createListIf(parallelMapper == null, threads);
        final int sz = values.size() / threads; // :NOTE: деление на 0
        final int remainder = values.size() % threads;
        final List<List<? extends T>> parts = createListIf(parallelMapper != null, threads);
        for (int i = 0; i < threads; ++i) {
            final int ind = i;
            final int start = sz * ind + Math.min(remainder, ind);
            final int end = start + sz + (ind < remainder ? 1 : 0);
            final List<? extends T> part = values.subList(start, end);
            if (parallelMapper == null) {
                Thread thread = new Thread(() -> threadsResults.set(ind, part.stream().map(mapper).
                        reduce(zeroSupplier.get(), accumulator, combiner)));
                threadsList.set(i, thread);
                thread.start();
            } else {
                parts.set(i, part);
            }
        }
        final List<S> finalThreadsResults;
        if (parallelMapper == null) {
            for (Thread thread : threadsList) {
                if (Thread.currentThread().isInterrupted()) {
                    stopThreads(threadsList, null);
                }
                try {
                    thread.join(); // :NOTE: worker-потоки тоже останавливать
                } catch (InterruptedException e) {
                    stopThreads(threadsList, e);
                }
            }
            finalThreadsResults = threadsResults;
        } else {
            finalThreadsResults = parallelMapper.map((it) -> it.stream().map(mapper).reduce(zeroSupplier.get(), accumulator, combiner), parts);
        }
        return finalThreadsResults.stream().reduce(zeroSupplier.get(), combiner);
    }

    private void stopThreads(List<Thread> threadsList, InterruptedException suppressed) throws InterruptedException {
        threadsList.forEach(Thread::interrupt);
        InterruptedException exception = new InterruptedException();
        if (suppressed != null) {
            exception.addSuppressed(suppressed);
        }
        threadsList.forEach(it -> {
            try {
                it.join();
            } catch (InterruptedException e) {
                exception.addSuppressed(e);
            }
        });
        throw exception;
    }
}
