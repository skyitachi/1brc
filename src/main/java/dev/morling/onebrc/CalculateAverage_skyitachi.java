package dev.morling.onebrc;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

/**
 * @author yaosp@trip.com
 * @date 2025/3/3
 */
public class CalculateAverage_skyitachi {

    private static final Path FILE = Path.of("./measurements.txt");
    private static final byte COLON = ';';
    private static final byte NEW_LINE = '\n';
    private static final byte HYPHEN = '-';
    private static final byte DOT = '.';
    private static final int NUM_OF_THREADS = Runtime.getRuntime().availableProcessors();

    static class State {
        public Map<String, double[]> stats = new HashMap<>();
        public byte[] parseBuffer = new byte[4096];
        public String currentKey = "";
        public double currentValue = 0;
        public int currentOffset = 0;
        public boolean isInKey = true;
    }

    public static void main(String[] args) throws IOException, ExecutionException, InterruptedException {
        CopyOnWriteArrayList<Map<String, double[]>> results = new CopyOnWriteArrayList<>();
        // int threads = Runtime.getRuntime().availableProcessors();
        int threads = 1;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        List<Future<Map<String, double[]>>> futures = new ArrayList<>();
        try (RandomAccessFile raf = new RandomAccessFile(FILE.toFile(), "rw")) {
            FileChannel channel = raf.getChannel();
            long end = channel.size();
            Future<Map<String, double[]>> future = executor.submit(() -> {
                return parseSingleThread(channel, 0, end);
            });
            futures.add(future);
        }
        Map<String, double[]> finalResults = futures.get(0).get();
        for (int i = 1; i < futures.size(); i++) {
            Map<String, double[]> singleResult = futures.get(i).get();
            for (Map.Entry<String, double[]> entry : singleResult.entrySet()) {
                String key = entry.getKey();
                double[] newValues = entry.getValue();
                if (finalResults.containsKey(key)) {
                    double[] finalValues = finalResults.get(key);
                    finalValues[0] += newValues[0];
                    finalValues[1] += newValues[1];
                    if (finalValues[3] > newValues[3]) {
                        finalValues[3] = newValues[3];
                    }
                    if (finalValues[2] < newValues[2]) {
                        finalValues[2] = newValues[2];
                    }
                }
                else {
                    finalResults.put(key, newValues);
                }
            }
        }
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        boolean first = true;
        for (Map.Entry<String, double[]> entry : finalResults.entrySet()) {
            double[] values = entry.getValue();
            double avg = values[0] / values[1];
            if (!first) {
                sb.append(", ");
            }
            else {
                first = false;
            }
            sb.append(entry.getKey())
                    .append("=").append(round(values[3])).append("/")
                    .append(round(avg)).append("/")
                    .append(round(values[2]));
        }
        sb.append("}");
        System.out.println(sb);

    }

    public static void parseSingleThread() throws IOException {
        State state = new State();
        try (RandomAccessFile raf = new RandomAccessFile(String.valueOf(FILE), "r")) {
            FileChannel channel = raf.getChannel();
            ByteBuffer buffer = ByteBuffer.allocate(4096);
            while (true) {
                int sz = channel.read(buffer);
                if (sz == -1) {
                    break;
                }
                parseBlock(buffer, state);
                if (sz < 4096) {
                    break;
                }
            }
        }
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        boolean first = true;
        for (Map.Entry<String, double[]> entry : state.stats.entrySet()) {
            double[] values = entry.getValue();
            double avg = values[0] / values[1];
            if (!first) {
                sb.append(", ");
            }
            else {
                first = false;
            }
            sb.append(entry.getKey())
                    .append("=").append(round(values[3])).append("/")
                    .append(round(avg)).append("/")
                    .append(round(values[2]));
        }
        sb.append("}");
        System.out.println(sb);
    }

    public static Map<String, double[]> parseSingleThread(FileChannel channel, long start, long end) throws IOException {
        State state = new State();
        channel.position(start);
        ByteBuffer buffer = ByteBuffer.allocate(4096);
        while (true) {
            long position = channel.position();
            long sz = 4096;
            buffer.limit((int) sz);
            if (end - position < 4096) {
                sz = end - position;
                buffer.limit((int) sz);
            }
            sz = channel.read(buffer);
            if (sz <= 0) {
                break;
            }
            parseBlock(buffer, state);
        }
        return state.stats;
    }

    public static void parseBlock(ByteBuffer buffer, State state) {
        buffer.flip();
        while (buffer.hasRemaining()) {
            byte b = buffer.get();
            if (b == NEW_LINE) {
                String valueString = new String(state.parseBuffer, 0, state.currentOffset);
                state.currentValue = Double.parseDouble(valueString);
                if (state.stats.containsKey(state.currentKey)) {
                    double[] values = state.stats.get(state.currentKey);
                    values[0] += state.currentValue;
                    values[1] += 1;
                    if (state.currentValue > values[2]) {
                        values[2] = state.currentValue;
                    }
                    if (state.currentValue < values[3]) {
                        values[3] = state.currentValue;
                    }
                }
                else {
                    double[] values = new double[4];
                    values[0] = state.currentValue;
                    values[1] = 1;
                    values[2] = state.currentValue;
                    values[3] = state.currentValue;
                    state.stats.put(state.currentKey, values);
                }
                state.isInKey = true;
                state.currentOffset = 0;
            }
            else if (b == COLON) {
                if (state.isInKey) {
                    state.currentKey = new String(state.parseBuffer, 0, state.currentOffset);
                    state.isInKey = false;
                    state.currentOffset = 0;
                }
            }
            else {
                state.parseBuffer[state.currentOffset] = b;
                state.currentOffset++;
            }
        }
        buffer.clear();
    }

    private static String round(double value) {
        return String.format("%.1f", value);
    }

}
