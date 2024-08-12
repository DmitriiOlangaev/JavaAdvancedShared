package info.kgeorgiy.ja.olangaev.crawler;

import info.kgeorgiy.ja.olangaev.myutils.MyUtilities;
import info.kgeorgiy.java.advanced.crawler.*;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Phaser;

import static info.kgeorgiy.ja.olangaev.myutils.MyUtilities.getArgument;

public class WebCrawler implements Crawler {
    private final ExecutorService downloadWorkers;
    private final ExecutorService extractWorkers;
    private final Downloader downloader;

    private final Map<String, PerHostController> perHostControl = new ConcurrentHashMap<>();
    private final int perHost;
    private volatile boolean isClosed = false;

    public WebCrawler(Downloader downloader, int downloaders, int extractors, int perHost) {
        getException(downloaders, "Downloaders");
        getException(extractors, "Extractors");
        getException(perHost, "PerHost");
        this.downloader = downloader;
        this.perHost = perHost;
        downloadWorkers = Executors.newFixedThreadPool(downloaders);
        extractWorkers = Executors.newFixedThreadPool(extractors);
    }

    /*private static <T> T getArgument(String[] args, int ind, Function<? super String, ? extends T> parser, T defaultValue) {
        return ind < args.length ? parser.apply(args[ind]) : defaultValue;
    }*/

    /**
     * The main method serves as the entry point for the WebCrawler program. Parses command line arguments and uses them to initialize a new WebCrawler with specified download depth,
     * number of concurrent downloads, extractors, and maximum number of connections per host.
     * Then calls the download method on the initialized WebCrawler object to crawl the given URL and retrieve web pages to the specified depth.
     *
     * @param args an array of strings containing command line arguments in the following format:
     *             "WebCrawler url [depth [downloads [extractors [perHost]]]]"
     *             where
     *             url is a required argument specifying the starting URL for crawling
     *             depth is an optional integer argument specifying the maximum depth to crawl, defaults to Integer.MAX_VALUE if not provided
     *             downloads is an optional integer argument specifying the maximum number of concurrent downloads, defaults to the number of available processors divided by 2 if not provided
     *             extractors is an optional integer argument specifying the number of extractors to use, defaults to the minimum of downloads and the number of available processors minus downloads if not provided
     *             perHost is an optional integer argument specifying the maximum number of connections per host, defaults to Integer.MAX_VALUE if not provided
     */

    public static void main(String[] args) {
        if (args == null || args.length < 1 || args.length > 5) {
            System.err.println("Your arguments don't match \"WebCrawler url [depth [downloads [extractors [perHost]]]]\"");
            return;
        }
        String url = args[0];
        int depth = getArgument(args, 1, Integer::parseInt, Integer.MAX_VALUE);
        int availableProcessors = Runtime.getRuntime().availableProcessors();
        int downloads = getArgument(args, 2, Integer::parseInt, availableProcessors / 2);
        int extractors = getArgument(args, 3, Integer::parseInt, Integer.min(downloads, availableProcessors - downloads));
        int perHost = getArgument(args, 4, Integer::parseInt, Integer.MAX_VALUE);
        try (WebCrawler webCrawler = new WebCrawler(new CachingDownloader(1), downloads, extractors, perHost)) {
            Result result = webCrawler.download(url, depth);
            System.out.println("Downloaded: " + result.getDownloaded().toString());
            System.out.println("Errors: " + result.getErrors().toString());
        } catch (IOException e) {
            System.err.println(e.getMessage());
        }
    }

    @Override
    public Result download(String url, int depth) {
        getException(depth, "Depth");
        return iterativeDownload(url, depth);
    }

    private void getException(int value, String name) {
        if (value <= 0) {
            throw new IllegalArgumentException(name + "must be more than zero");
        }
    }

    private Result iterativeDownload(final String url, final int depth) {
        final Map<String, Boolean> used = new ConcurrentHashMap<>();
        final Map<String, IOException> errors = new ConcurrentHashMap<>();
        final Phaser phaser = new Phaser(1);
        Set<String> cur = new HashSet<>(List.of(url));
        for (int i = 1; i <= depth && !isClosed; ++i) {
            Set<String> newCur = ConcurrentHashMap.newKeySet();
            cur.parallelStream().filter(it -> !used.containsKey(it)).forEach(it ->
            {
                try {
                    final String host = URLUtils.getHost(it);
                    final PerHostController perHostController = perHostControl.computeIfAbsent(host, h -> new PerHostController(perHost));
                    phaser.register();
                    perHostController.run(() -> {
                        try {
                            final Document document = downloader.download(it);
                            used.put(it, true);
                            phaser.register();
                            extractWorkers.submit(() -> {
                                try {
                                    newCur.addAll(document.extractLinks());
                                } catch (IOException e) {
                                    errors.put(it, e);
                                } finally {
                                    phaser.arriveAndDeregister();
                                }
                            });
                        } catch (IOException e) {
                            used.put(it, false);
                            errors.put(it, e);
                        } finally {
                            phaser.arriveAndDeregister();
                        }
                    });
                } catch (MalformedURLException e) {
                    used.put(it, false);
                    errors.put(it, e);
                }
            });
            phaser.arriveAndAwaitAdvance();
            cur = newCur;
        }
        return new Result(used.entrySet().parallelStream().filter(Map.Entry::getValue).map(Map.Entry::getKey).toList(), errors);
    }

    @Override
    public void close() {
        isClosed = true;
        MyUtilities.shutdownNowAndAwait(downloadWorkers, extractWorkers);
    }

    private class PerHostController {
        private final Queue<Runnable> queue;
        private final int perHost;
        private int cnt = 0;

        public PerHostController(int perHost) {
            this.perHost = perHost;
            queue = new ArrayDeque<>();
        }

        public synchronized void run(Runnable r) {
            if (cnt == perHost) {
                queue.add(r);
            } else {
                ++cnt;
                downloadWorkers.submit(() -> {
                    r.run();
                    synchronized (this) {
                        --cnt;
                        final Runnable nr = queue.poll();
                        if (nr != null) {
                            run(nr);
                        }
                    }
                });
            }
        }
    }
}
