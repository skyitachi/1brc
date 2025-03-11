package dev.morling.onebrc;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

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

    private static Map<String, double[]> stats = new HashMap<>();

    private static byte[] parseBuffer = new byte[4096];
    private static String currentKey = "";
    private static double currentValue = 0;
    private static int currentOffset = 0;
    private static boolean isInKey = true;

    public static void main(String []args) throws IOException {
        try (RandomAccessFile raf = new RandomAccessFile(String.valueOf(FILE), "r")) {
            FileChannel channel = raf.getChannel();
            ByteBuffer buffer = ByteBuffer.allocate(4096);
            channel.read(buffer);
            parseBlock(buffer);
        }
        for(Map.Entry<String, double[]> entry : stats.entrySet()) {
            double[] values = entry.getValue();
            double avg = values[0] / values[1];
            System.out.println(entry.getKey() + ": " + avg + ", max: " + values[2] + ", min: " + values[3]);
        }
    }

    /**
     */
    public static void parseBlock(ByteBuffer buffer) {
        buffer.flip();
        while (buffer.hasRemaining()) {
            byte b = buffer.get();
            if (b == NEW_LINE) {
                // 判断是否要parse left buffer里的数据
                String valueString = new String(parseBuffer, 0, currentOffset);
                currentValue = Double.parseDouble(valueString);
                if (stats.containsKey(currentKey)) {
                    double[] values = stats.get(currentKey);
                    values[0] += currentValue;
                    values[1] += 1;
                    if (currentValue > values[2]) {
                        values[2] = currentValue;
                    }
                    if (currentValue < values[3]) {
                        values[3] = currentValue;
                    }
                } else {
                    double[] values = new double[4];
                    values[0] = currentValue;
                    values[1] = 1;
                    values[2] = currentValue;
                    values[3] = currentValue;
                    stats.put(currentKey, values);
                }
                isInKey = true;
            } else if (b == COLON) {
                if (isInKey) {
                  currentKey = new String(parseBuffer, 0, currentOffset);
                  isInKey = false;
                  currentOffset = 0;
                }
            } else {
                parseBuffer[currentOffset] = b;
                currentOffset++;
            }
        }
        buffer.flip();
    }



}
