package info.kgeorgiy.ja.olangaev.hello;

import info.kgeorgiy.java.advanced.hello.HelloClient;

import java.io.IOException;
import java.nio.channels.DatagramChannel;

public class HelloUDPNonBlockingClient implements HelloClient {
    private DatagramChannel[] datagramChannels;
    @Override
    public void run(String host, int port, String prefix, int threads, int requests) {
        datagramChannels = new DatagramChannel[threads];
        for(int i = 0; i < threads; ++i) {
            try {
                datagramChannels[i] = DatagramChannel.open();
            } catch (IOException e) {
                release();
                throw new RuntimeException(e);
            }
        }
    }

    private void release() {

    }
}
