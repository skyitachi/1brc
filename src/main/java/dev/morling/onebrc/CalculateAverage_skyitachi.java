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

    private static byte[] left = new byte[4096];

    public static void main(String []args) throws IOException {
        try (RandomAccessFile raf = new RandomAccessFile(String.valueOf(FILE), "r")) {
            FileChannel channel = raf.getChannel();
            ByteBuffer buffer = ByteBuffer.allocate(4096);
            channel.read(buffer);
        }
    }

    /**
     * @param buffer 需要解析的buffer，不包含left buffer
     * @param status, 0: 不需要parse left buffer，1：需要parse，且没解析完key，2: 需要parse，需要解析value, 3: left buffer已经parse完成
     * @param leftover, left buffer里的总长度
     * @param offset, value在left buffer里的offset
     */
    public static void parseBlock(ByteBuffer buffer, int status, int leftover, int offset) {
        buffer.flip();
        boolean isInKey = true;
        if (status == 2) {
            isInKey = false;
        }
        int currentKeyLen = 0;
        if (status != 0) {
            currentKeyLen = offset;
        }
        int currentValueOffset = 0;
        while (buffer.hasRemaining()) {
            byte b = buffer.get();
            if (b == NEW_LINE) {
                // 判断是否要parse left buffer里的数据
                if (status == 0) {
                    // 解析value
                }
                continue;
            }
        }
    }



}
