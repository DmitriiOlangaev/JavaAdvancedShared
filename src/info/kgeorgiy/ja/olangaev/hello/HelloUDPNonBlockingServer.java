package info.kgeorgiy.ja.olangaev.hello;

import info.kgeorgiy.ja.olangaev.myutils.MyPair;
import info.kgeorgiy.ja.olangaev.myutils.MyUtilities;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.ArrayDeque;
import java.util.Queue;
import java.util.concurrent.*;

public class HelloUDPNonBlockingServer extends AbstractUDPServer {
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private DatagramChannel channel;
    private Selector selector;
    private ByteBuffer mainBuffer;
    private BlockingQueue<MyPair<SocketAddress, byte[]>> responsesQueue;
    private BlockingQueue<ByteBuffer> workersBuffers;
    private Phaser receiveBufferRelease;


    private void receive(final SelectionKey key) {
        try {
            mainBuffer.clear();
            final SocketAddress client = channel.receive(mainBuffer);
            if (client == null) {
                return;
            }
            mainBuffer.flip();
            workers.submit(() -> handleRequest(client, key));
            receiveBufferRelease.arriveAndAwaitAdvance();
        } catch (IOException e) {
            System.err.println("Error while writing response to buffer occurred" + System.lineSeparator() + e.getMessage());
        }
    }

    private void handleRequest(SocketAddress dest, SelectionKey key) {
        ByteBuffer buffer = workersBuffers.poll();
        if (buffer == null) {
            throw new IllegalStateException("buffer in handle request is null");
        }
        buffer.clear();
        buffer.put(Answer);
        buffer.put(mainBuffer);
        buffer.flip();
        receiveBufferRelease.arrive();
        byte[] response = new byte[buffer.limit()];
        buffer.get(response);
        if (responsesQueue.offer(new MyPair<>(dest, response))) {
            key.interestOpsOr(SelectionKey.OP_WRITE);
            selector.wakeup();
        }
        workersBuffers.add(buffer);
    }

    private void send(SelectionKey key) {
        final MyPair<SocketAddress, byte[]> response = responsesQueue.poll();
        if (response == null) {
            throw new IllegalStateException("Response in send is null");
        }
        key.interestOpsOr(SelectionKey.OP_READ);
        selector.wakeup();
        mainBuffer.clear();
        mainBuffer.put(response.getSecond());
        mainBuffer.flip();
        try {
            channel.send(mainBuffer, response.getFirst());
        } catch (IOException e) {
            System.err.println("Error while sending response occurred" + System.lineSeparator() + e.getMessage());
        }
    }

    private void keyConsumer(SelectionKey key) {
        if (!key.isValid()) {
            return;
        }
        if (key.isReadable()) {
            if (responsesQueue.remainingCapacity() == 0) {
                key.interestOpsAnd(SelectionKey.OP_WRITE);
            } else {
                receive(key);
            }
        }
        if (key.isWritable()) {
            if (responsesQueue.size() == 0) {
                key.interestOpsAnd(SelectionKey.OP_READ);
            } else {
                send(key);
            }
        }
    }

    private void startListen() {
        worker.submit(() -> {
            while (isStarted) {
                try {
                    selector.select(this::keyConsumer);
                } catch (IOException e) {
                    System.err.println("Error while selection key occurred");
                }
            }
        });
    }

    @Override
    public void start(int port, int threads) {
        super.start(port, threads);
        try {
            channel = DatagramChannel.open();
            channel.bind(new InetSocketAddress(port));
            buffSize = channel.socket().getReceiveBufferSize();
            channel.configureBlocking(false);
            selector = Selector.open();
            channel.register(selector, SelectionKey.OP_READ);
            mainBuffer = ByteBuffer.allocate(buffSize);
            responsesQueue = new ArrayBlockingQueue<>(4 * threads);
            workersBuffers = new ArrayBlockingQueue<>(threads);
            receiveBufferRelease = new Phaser(2);
            for (int i = 0; i < threads; ++i) {
                workersBuffers.add(ByteBuffer.allocate(buffSize));
            }
            startListen();
        } catch (IOException e) {
            System.err.println("Error while starting server occurred");
            close();
            throw new RuntimeException(e);
        }
    }

    @Override
    public void close() {
        try {
            if (channel != null) {
                channel.close();
            }
            if (selector != null) {
                selector.close();
            }
            MyUtilities.shutdownAndAwait(worker);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        super.close();
    }

}
