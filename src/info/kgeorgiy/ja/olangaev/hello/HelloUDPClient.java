package info.kgeorgiy.ja.olangaev.hello;

import info.kgeorgiy.ja.olangaev.myutils.MyUtilities;

public class HelloUDPClient implements info.kgeorgiy.java.advanced.hello.HelloClient {

    /**
     * The main method is the entry point for the HelloUDPClient application.
     * It retrieves command-line arguments specifying the server host, port number, prefix for messages,
     * number of threads and number of requests to send to the server. If any of these arguments are missing
     * or not in the correct format, an error message is printed and the program is terminated.
     * Once the input arguments are validated, a new instance of HelloUDPClient is created and its run() method
     * is called with the provided parameters for sending messages to the specified server.
     *
     * @param args an array of string arguments containing server host, port number, prefix for messages,
     *             <p>
     *             number of threads and number of requests to send to the server.
     */
    public static void main(String[] args) {
        if (args == null || args.length != 5) {
            System.err.println("Arguments must have size 5");
            return;
        }
        String host = args[0];
        int port = MyUtilities.getArgument(args, 1, Integer::parseInt, null);
        String prefix = args[2];
        int threads = MyUtilities.getArgument(args, 3, Integer::parseInt, null);
        int requests = MyUtilities.getArgument(args, 4, Integer::parseInt, null);
        new HelloUDPClient().run(host, port, prefix, threads, requests);
    }

    @Override
    public void run(String host, int port, String prefix, int threads, int requests) {
        new ClientRunner(host, port, prefix, threads, requests).run();
    }
}
