package info.kgeorgiy.ja.olangaev.hello;

import info.kgeorgiy.ja.olangaev.myutils.MyUtilities;
import info.kgeorgiy.java.advanced.hello.HelloServer;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HelloUDPServer implements HelloServer {
    private final static byte[] Answer = ("Hello, ").getBytes(StandardCharsets.UTF_8);
    private volatile boolean isStarted = false;
    private ExecutorService workers;
    private DatagramSocket socket;

    /**
     * The main method is the entry point of the program that starts the HelloUDPServer.
     * <p>
     * It takes in an array of arguments, checks if it has two arguments, and then extracts two integers from them.
     * <p>
     * Once the arguments are validated, a new instance of HelloUDPServer is created and started with given port and threads.
     *
     * @param args an array of command-line arguments
     */
    public static void main(String[] args) {
        if (args == null || args.length != 2) {
            System.err.println("Arguments must have size 2");
            return;
        }
        int port = MyUtilities.getArgument(args, 0, Integer::parseInt, null);
        int threads = MyUtilities.getArgument(args, 1, Integer::parseInt, null);
        new HelloUDPServer().start(port, threads);
    }

    @Override
    public synchronized void start(int port, int threads) {
        MyUtilities.checkBounds("Port", port, 0, 65535);
        MyUtilities.checkBounds("Threads", threads, 0, Integer.MAX_VALUE);
        if (isStarted) {
            throw new IllegalStateException("Server is already started, you should close previous one");
        }
        isStarted = true;
        workers = Executors.newFixedThreadPool(threads);
        try {
            socket = new DatagramSocket(port);
            final int bufSize = socket.getReceiveBufferSize();
            for (int i = 0; i < threads; ++i) {
                final DatagramPacket packet = new DatagramPacket(new byte[bufSize], bufSize);
                workers.submit(() -> {
                    receiveAndSend(packet);
                });
            }
        } catch (SecurityException |
                 SocketException e) {
            System.err.println("Can't bind socket");
            System.err.println(e.getMessage());
        }
    }

    private void receiveAndSend(final DatagramPacket packet) {
        while (isStarted) {
            try {
                socket.receive(packet);
                System.arraycopy(packet.getData(), 0, packet.getData(), Answer.length, packet.getLength());
                System.arraycopy(Answer, 0, packet.getData(), 0, Answer.length);
                packet.setLength(packet.getLength() + Answer.length);
                try {
                    socket.send(packet);
                } catch (Exception e) {
                    System.err.println("Error during sending packet respond occurred " + e.getMessage());
                }
            } catch (Exception e) {
                System.err.println("Error during receiving packet occurred " + e.getMessage());
            }
        }
    }

    @Override
    public synchronized void close() {
        if (isStarted) {
            isStarted = false;
            socket.close();
            MyUtilities.shutdownAndAwait(workers);
        }
    }
}
