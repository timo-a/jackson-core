package com.fasterxml.jackson.core.json.async;

import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import com.fasterxml.jackson.core.*;

import org.junit.jupiter.api.Test;
import com.fasterxml.jackson.core.async.AsyncTestBase;
import com.fasterxml.jackson.core.testsupport.AsyncReaderWrapper;

import static org.junit.jupiter.api.Assertions.fail;

public class AsyncConcurrencyTest extends AsyncTestBase
{
    final static JsonFactory JSON_F = new JsonFactory();
    static {
        // To make it pass, try:
//        JSON_F.disable(JsonFactory.Feature.USE_THREAD_LOCAL_FOR_BUFFER_RECYCLING);
    }

    private final static String TEXT1 = "Short";
    private final static String TEXT2 = "Some longer text";
    private final static String TEXT3 = "and yet more";
    private final static String TEXT4 = "... Longest yet although not superbly long still (see 'apos'?)";

    final static byte[] JSON_DOC = utf8Bytes(String.format(
            "[\"%s\", \"%s\",\n\"%s\",\"%s\" ]", TEXT1, TEXT2, TEXT3, TEXT4));

    class WorkUnit implements AutoCloseable {
        private int stage = 0;

        private AsyncReaderWrapper parser;

        private boolean errored = false;

        public boolean process() throws Exception {
            // short-cut through if this instance has already failed
            if (errored) {
                return false;
            }
            try {
                switch (stage++) {
                case 0:
                    parser = createParser();
                    break;
                case 1:
                    _assert(JsonToken.START_ARRAY);
                    break;
                case 2:
                    _assert(TEXT1);
                    break;
                case 3:
                    _assert(TEXT2);
                    break;
                case 4:
                    _assert(TEXT3);
                    break;
                case 5:
                    _assert(TEXT4);
                    break;
                case 6:
                    _assert(JsonToken.END_ARRAY);
                    break;
                default:
                    /*
                    if (parser.nextToken() != null) {
                        throw new IOException("Unexpected token at "+stage+"; expected `null`, got "+parser.currentToken());
                    }
                    */
                    close();
                    return true;
                }
            } catch (Exception e) {
                errored = true;
                throw e;
            }
            return false;
        }

        private void _assert(String exp) throws IOException {
            _assert(JsonToken.VALUE_STRING);
            String str = parser.currentText();
            if (!exp.equals(str)) {
                throw new IOException("Unexpected VALUE_STRING: expected '"+exp+"', got '"+str+"'");
            }
        }

        private void _assert(JsonToken exp) throws IOException {
            JsonToken t = parser.nextToken();
            if (t != exp) {
                throw new IOException("Unexpected token at "+stage+"; expected "+exp+", got "+t);
            }
        }

        @Override
        public void close() throws Exception {
            if (parser != null) {
                parser.close();
                parser = null;
                stage = 0;
            }
        }
    }

    // [jackson-core#476]
    @Test
    public void testConcurrentAsync() throws Exception
    {
        final int MAX_ROUNDS = 30;
        for (int i = 0; i < MAX_ROUNDS; ++i) {
            _testConcurrentAsyncOnce(i, MAX_ROUNDS);
        }
    }

    void _testConcurrentAsyncOnce(final int round, final int maxRounds) throws Exception
    {
        final int numThreads = 3;
        final ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        final AtomicInteger errorCount = new AtomicInteger(0);
        final AtomicInteger completedCount = new AtomicInteger(0);
        final AtomicReference<String> errorRef = new AtomicReference<>();

        // First, add a few shared work units
        final ArrayBlockingQueue<WorkUnit> q = new ArrayBlockingQueue<>(20);
        for (int i = 0; i < 7; ++i) {
            q.add(new WorkUnit());
        }

        // then invoke swarm of workers on it...

        final int REP_COUNT = 99000;
        ArrayList<Future<?>> futures = new ArrayList<>();
        for (int i = 0; i < REP_COUNT; i++) {
            Callable<Void> c = () -> {
                WorkUnit w = q.take();
                try {
                    if (w.process()) {
                        completedCount.incrementAndGet();
                    }
                } catch (Throwable t) {
                    if (errorCount.getAndIncrement() == 0) {
                        errorRef.set(t.toString());
                    }
                } finally {
                    q.add(w);
                }
                return null;
            };
            futures.add(executor.submit(c));
        }
        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);
        int count = errorCount.get();

        if (count > 0) {
            fail("Expected no problems (round "+round+"/"+maxRounds
                    +"); got "+count+", first with: "+errorRef.get());
        }
        final int EXP_COMPL = ((REP_COUNT + 7) / 8);
        int compl = completedCount.get();

        if (compl < (EXP_COMPL-10) || compl > EXP_COMPL) {
            fail("Expected about "+EXP_COMPL+" completed rounds, got: "+compl);
        }

        while (!q.isEmpty()) {
            q.take().close();
        }
    }

    protected AsyncReaderWrapper createParser() throws IOException {
        return asyncForBytes(JSON_F, 100, JSON_DOC, 0);
    }
}
