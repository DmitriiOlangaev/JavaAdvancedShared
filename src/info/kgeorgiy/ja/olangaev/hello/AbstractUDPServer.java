package info.kgeorgiy.ja.olangaev.hello;

import info.kgeorgiy.ja.olangaev.myutils.MyUtilities;
import info.kgeorgiy.java.advanced.hello.HelloServer;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public abstract class AbstractUDPServer implements HelloServer {
    protected final static byte[] Answer = ("Hello, ").getBytes(StandardCharsets.UTF_8);
    protected ExecutorService workers;

    protected volatile boolean isStarted = false;

    protected int buffSize;

    @Override
    public void start(int port, int threads) {
        MyUtilities.checkBounds("Port", port, 0, 65535);
        MyUtilities.checkBounds("Threads", threads, 0, Integer.MAX_VALUE);
        if (isStarted) {
            throw new IllegalStateException("Server is already started, you should close previous one");
        }
        isStarted = true;
        workers = Executors.newFixedThreadPool(threads);
    }

    @Override
    public synchronized void close() {
        if (isStarted) {
            isStarted = false;
            MyUtilities.shutdownAndAwait(workers);
        }
    }

}
