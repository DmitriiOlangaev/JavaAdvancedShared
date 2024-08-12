package info.kgeorgiy.ja.olangaev.hello;

import info.kgeorgiy.ja.olangaev.myutils.MyPair;
import info.kgeorgiy.ja.olangaev.myutils.MyUtilities;

import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Phaser;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ClientRunner {
    private final static int SOCKET_MIN_TIMEOUT = 25;
    private final static int SOCKET_MAX_TIMEOUT = 1000;
    private final static int SOCKET_TIME_OUT_STEP = 50;
    private final String prefix;
    private final int threads;
    private final int requests;
    private final SocketAddress dest;
    private final ExecutorService workers;
    private final Pattern responsePattern;
    private final ConcurrentHashMap<Integer, Integer> completed;
    private final Map<Integer, MyPair<String, String>> threadToRequestWithResponse;
    private final Phaser syncPhaser;
    private final AtomicInteger misses;
    private final int missLimit;
    private final DatagramSocket socket;

    public ClientRunner(String host, int port, String prefix, int threads, int requests) {
        MyUtilities.checkBounds("Port", port, 0, 65535);
        MyUtilities.checkBounds("Threads", threads, 0, Integer.MAX_VALUE);
        MyUtilities.checkBounds("Requests", requests, 0, Integer.MAX_VALUE);
        this.prefix = prefix;
        this.threads = threads;
        this.requests = requests;
        this.dest = new InetSocketAddress(host, port);
        this.workers = Executors.newFixedThreadPool(threads);
        this.responsePattern = Pattern.compile("^(\\p{L}+), " + prefix + "(\\p{Nd}+)_(\\p{Nd}+)$");
        this.completed = new ConcurrentHashMap<>();
        this.threadToRequestWithResponse = new ConcurrentHashMap<>();
        this.syncPhaser = new Phaser(threads);
        this.missLimit = threads;
        this.misses = new AtomicInteger(threads);
        try {
            this.socket = new DatagramSocket();
        } catch (SocketException e) {
            System.err.println("Can't create socket");
            throw new RuntimeException(e);
        }
    }

    private int prepare(int threadNumber) {
        final int requestNumber = completed.get(threadNumber);
        if (requestNumber > 1) {
            MyPair<String, String> res = threadToRequestWithResponse.get(threadNumber);
            if (res.getSecond() != null) {
                System.out.println("Request " + res.getFirst() + " " + "Response " + res.getSecond());
                res.setFirst(null);
                res.setSecond(null);
            }
        }
        syncPhaser.arriveAndAwaitAdvance();
        if (requestNumber == requests + 1) {
            completed.remove(threadNumber);
            threadToRequestWithResponse.remove(threadNumber);
            syncPhaser.arriveAndDeregister();
            return -1;
        }
        return requestNumber;
    }

    private void send(int threadNumber, int requestNumber) {
        final String request = prefix + threadNumber + "_" + requestNumber;
        threadToRequestWithResponse.get(threadNumber).setFirst(request);
        final byte[] buf = request.getBytes(StandardCharsets.UTF_8);
        final DatagramPacket packetSend = new DatagramPacket(buf, buf.length, dest);
        try {
            socket.send(packetSend);
        } catch (Exception e) {
            System.err.println("Error during packet sending occurred " + e.getMessage());
        }
    }

    private void receive(DatagramPacket packet) {
        try {
            final int mss = misses.getAndUpdate(it -> (Math.abs(it) > missLimit ? 0 : it));
            if (Math.abs(mss) > missLimit) {
                socket.setSoTimeout(Math.min(SOCKET_MAX_TIMEOUT,
                        Math.max(SOCKET_MIN_TIMEOUT,
                                socket.getSoTimeout() + Integer.signum(mss) * SOCKET_TIME_OUT_STEP)));
            }
            socket.receive(packet);
            misses.decrementAndGet();
            final String response = new String(packet.getData(), 0, packet.getLength(), StandardCharsets.UTF_8);
            final Matcher matcher = responsePattern.matcher(response);
            if (matcher.matches()) {
                try {
                    final int tNumber = Integer.parseInt(matcher.group(2));
                    final int rNumber = Integer.parseInt(matcher.group(3));
                    final MyPair<String, String> pair = threadToRequestWithResponse.get(tNumber);
                    if (pair != null && rNumber == completed.get(tNumber)) {
                        synchronized (pair) {
                            pair.setSecond(response);
                        }
                        completed.computeIfPresent(tNumber, (k, v) -> v + 1);
                    } else {
                        System.err.println("Server response have no pair request " + response);
                    }
                } catch (NumberFormatException e) {
                    System.err.println("Server response = " + response + "Got not homogeneous digits sequence" + System.lineSeparator() + e.getMessage());
                }
            } else {
                System.err.println("Server answer doesn't match required pattern, got " + response);
            }
        } catch (SocketTimeoutException e) {
            misses.incrementAndGet();
        } catch (Exception e) {
            System.err.println("Error during packet receiving occurred " + e.getMessage());
        } finally {
            syncPhaser.arriveAndAwaitAdvance();
        }
    }

    public void run() {
        try {
            socket.setSoTimeout(SOCKET_MIN_TIMEOUT);
            final int bufSize = socket.getReceiveBufferSize();

            for (int threadNumber = 1; threadNumber <= threads; ++threadNumber) {
                threadToRequestWithResponse.put(threadNumber, new MyPair<>(null, null));
                completed.put(threadNumber, 1);
                final int finalThreadNumber = threadNumber;
                DatagramPacket packetReceive = new DatagramPacket(new byte[bufSize], bufSize);
                workers.submit(() -> {
                    while (true) {
                        final int requestNumber = prepare(finalThreadNumber);
                        if (requestNumber == -1) {
                            break;
                        }
                        send(finalThreadNumber, requestNumber);
                        receive(packetReceive);
                    }
                });
            }
            MyUtilities.shutdownAndAwait(workers);
        } catch (SecurityException |
                 SocketException e) {
            System.err.println("Can't bind socket");
            System.err.println(e.getMessage());
        } finally {
            socket.close();
        }
    }
}
