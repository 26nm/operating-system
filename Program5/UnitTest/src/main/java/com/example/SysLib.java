package com.example;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.HashMap;
import java.util.Map;

public class SysLib {
    private static final String DISK_FILE_NAME = "disk.dat";
    private static final int DISK_SIZE = 1000; // Example disk size, adjust as needed
    private static Map<Integer, byte[]> diskCache = new HashMap<>();
    private static RandomAccessFile diskFile;
    private static FileChannel diskChannel;
    private static boolean diskInitialized = false;

    /**
     * Initializes the local disk.
     *
     * @throws IOException If an I/O error occurs.
     */
    public static synchronized void initDisk() throws IOException {
        if (!diskInitialized) {
            File file = new File(DISK_FILE_NAME);

            if (!file.exists()) {
                // Create a new disk file and fill it with zeros
                file.createNewFile();
                diskFile = new RandomAccessFile(file, "rw");
                diskChannel = diskFile.getChannel();
                diskFile.setLength(DISK_SIZE * Disk.blockSize);

                // Fill with zeros to simulate an empty disk
                ByteBuffer buffer = ByteBuffer.allocate(Disk.blockSize);
                for (int i = 0; i < DISK_SIZE; i++) {
                    diskChannel.write(buffer, i * Disk.blockSize);
                    buffer.rewind();
                }
            } else {
                diskFile = new RandomAccessFile(file, "rw");
                diskChannel = diskFile.getChannel();

            }

            diskInitialized = true;
        }
        clearCache(); // add this line to clear the cache.
    }

    /**
     * Reads a raw block from the simulated disk.
     *
     * @param blockId The ID of the block to read.
     * @param buffer  The buffer to read the data into.
     * @return 0 on success, -1 on failure.
     */
    public static synchronized int rawread(int blockId, byte[] buffer) {
        if (!diskInitialized) {
            try {
                initDisk();
            } catch (IOException e) {
                cerr("Error initializing disk: " + e.getMessage() + "\n");
                return -1;
            }
        }
        if (blockId < 0 || blockId >= DISK_SIZE || buffer.length != Disk.blockSize) {
            cerr("Invalid blockId or buffer size in rawread: blockId=" + blockId + ", buffer.length=" + buffer.length
                    + "\n");
            return -1;
        }

        try {
            // Check if the block is in the cache
            if (diskCache.containsKey(blockId)) {
                System.arraycopy(diskCache.get(blockId), 0, buffer, 0, Disk.blockSize);
                return 0;
            }

            // Read directly from the disk file
            ByteBuffer bb = ByteBuffer.wrap(buffer);
            diskChannel.read(bb, blockId * Disk.blockSize);
            // Add block to cache.
            diskCache.put(blockId, buffer.clone());

            return 0;
        } catch (IOException e) {
            cerr("Error reading block: " + blockId + ": " + e.getMessage() + "\n");
            return -1;
        }
    }

    /**
     * Writes a raw block to the simulated disk.
     *
     * @param blockId The ID of the block to write to.
     * @param buffer  The data to write.
     * @return 0 on success, -1 on failure.
     */
    public static synchronized int rawwrite(int blockId, byte[] buffer) {
        if (!diskInitialized) {
            try {
                initDisk();
            } catch (IOException e) {
                cerr("Error initializing disk: " + e.getMessage() + "\n");
                return -1;
            }
        }
        if (blockId < 0 || blockId >= DISK_SIZE || buffer.length != Disk.blockSize) {
            cerr("Invalid blockId or buffer size in rawwrite: blockId=" + blockId + ", buffer.length=" + buffer.length
                    + "\n");
            return -1;
        }

        try {
            // Write to the cache
            diskCache.put(blockId, buffer.clone());
            // Zero out the block before writing new data
            ByteBuffer cleanBuffer = ByteBuffer.allocate(Disk.blockSize);
            diskChannel.write(cleanBuffer, blockId * Disk.blockSize);
            // Copy new data into the block
            ByteBuffer bb = ByteBuffer.wrap(buffer);
            diskChannel.write(bb, blockId * Disk.blockSize);
            return 0;
        } catch (IOException e) {
            cerr("Error writing block: " + blockId + ": " + e.getMessage() + "\n");
            return -1;
        }
    }

    /**
     * Converts an integer to a byte array.
     *
     * @param value  The integer value to convert.
     * @param buffer The byte array to store the result.
     * @param offset The starting offset in the buffer.
     */
    public static void int2bytes(int value, byte[] buffer, int offset) {
        ByteBuffer.wrap(buffer, offset, 4).putInt(value);
    }

    /**
     * Converts a byte array to an integer.
     *
     * @param buffer The byte array to read from.
     * @param offset The starting offset in the buffer.
     * @return The integer value.
     */
    public static int bytes2int(byte[] buffer, int offset) {
        return ByteBuffer.wrap(buffer, offset, 4).getInt();
    }

    /**
     * Converts a short to a byte array.
     *
     * @param value  The short value to convert.
     * @param buffer The byte array to store the result.
     * @param offset The starting offset in the buffer.
     */
    public static void short2bytes(short value, byte[] buffer, int offset) {
        ByteBuffer.wrap(buffer, offset, 2).putShort(value);
    }

    /**
     * Converts a byte array to a short.
     *
     * @param buffer The byte array to read from.
     * @param offset The starting offset in the buffer.
     * @return The short value.
     */
    public static short bytes2short(byte[] buffer, int offset) {
        return ByteBuffer.wrap(buffer, offset, 2).getShort();
    }

    /**
     * Prints an error message to the standard error stream.
     *
     * @param message The message to print.
     */
    public static void cerr(String message) {
        System.err.print(message);
    }

    public static void clearCache() {
        diskCache.clear();
    }
}