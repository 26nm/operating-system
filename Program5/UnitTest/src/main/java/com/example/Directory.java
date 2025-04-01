package com.example;
import java.util.Arrays;
/**
 * The {@code Directory} class represents the directory structure of the file
 * system.
 * It manages file names and their corresponding inode numbers.
 * This class provides methods to allocate and deallocate inode numbers,
 * convert the directory structure to and from byte arrays for disk storage,
 * and look up inode numbers by file name.
 * 
 * By: Nolan Dela Rosa
 * 
 * March 20, 2025
 */
public class Directory {
    private static final int MAX_CHARS = Constants.MAX_FILENAME_LENGTH;
    private boolean[] inodeUsed;
    private int[] fileNameSizes;
    private String[] fileNames;
    
    /**
     * Constructs a {@code Directory} object with a maximum number of inodes.
     *
     * @param maxInumber The maximum number of inodes (files) that can be stored in
     *                   this directory.
     *                   This value determines the size of the {@code filenameSizes}
     *                   array.
     */
    public Directory(int maxInumber) {
        inodeUsed = new boolean[maxInumber];
        fileNameSizes = new int[maxInumber];
        fileNames = new String[maxInumber];
        fileNames[0] = "/";
        fileNameSizes[0] = 1;
    }

    /**
     * Initializes the directory from a byte array read from the disk.
     *
     * @param data The byte array containing the directory information read from the
     *             disk.
     *             The data is expected to be formatted as a sequence of file sizes
     *             followed by a sequence of file names.
     */
    public void bytes2directory(byte data[]) {
        int offset = 0;

        for(int i = 0; i < fileNameSizes.length; i++) {
            fileNameSizes[i] = SysLib.bytes2int(data, offset);
            offset += 4;
        }

        for(int i = 0; i < fileNames.length; i++) {
            byte[] nameBytes = new byte[MAX_CHARS * 2];
            System.arraycopy(data, offset, nameBytes, 0, MAX_CHARS * 2);
            fileNames[i] = new String(nameBytes).trim();
            offset += MAX_CHARS * 2;
        }
    }

    /**
     * Converts the directory information into a byte array for storage on disk.
     *
     * @return A byte array representing the directory data, ready to be written to
     *         the disk.
     *         The byte array is formatted as a sequence of file sizes followed by a
     *         sequence of file names.
     */
    public byte[] directory2bytes() {
        byte[] data = new byte[(fileNameSizes.length * 4) + (fileNames.length * MAX_CHARS * 2)];
        int offset = 0;

        for(int size : fileNameSizes) {
            SysLib.int2bytes(size, data, offset);
            offset += 4;
        }

        for(String name : fileNames) {
            byte[] nameBytes = (name != null ? name : " ").getBytes();
            byte[] paddedName = new byte[MAX_CHARS * 2];
            System.arraycopy(nameBytes, 0, paddedName, 0, Math.min(nameBytes.length, MAX_CHARS * 2));
            System.arraycopy(paddedName, 0, data, offset, MAX_CHARS * 2);
            offset += MAX_CHARS * 2;
        }

        return data;
    }

    /**
     * Allocates a new inode number for a given file name.
     *
     * @param filename The name of the file for which to allocate an inode number.
     * @return The allocated inode number if successful; otherwise, -1 if no inode
     *         numbers are available.
     */
    public short ialloc(String filename) {
        if(filename.length() > MAX_CHARS) {
            filename = filename.substring(0, MAX_CHARS);
        }

        for(short i = 1; i < inodeUsed.length; i++) {
            if(!inodeUsed[i]) {
                inodeUsed[i] = true;
                fileNames[i] = filename;
                fileNameSizes[i] = (short)filename.length();
                return i;
            }
        }
        return -1;
    }

    /**
     * Deallocates an inode number, effectively deleting the corresponding file.
     *
     * @param iNodeIndex The inode index number to deallocate.
     * @return {@code true} if the inode number was successfully deallocated;
     *         {@code false} if the inode number was not in use.
     */
    public boolean ifree(short iNodeIndex) {
        if(iNodeIndex >= 0 && iNodeIndex < inodeUsed.length && inodeUsed[iNodeIndex]) {
            inodeUsed[iNodeIndex] = false;
            fileNames[iNodeIndex] = null;
            fileNameSizes[iNodeIndex] = 0;
            return true;
        }

        return false;
    }

    /**
     * Retrieves the inode number associated with a given file name.
     *
     * @param filename The name of the file to look up.
     * @return The inode number corresponding to the file name if found; otherwise,
     *         -1.
     */
    public short namei(String filename) {
        for(short i = 0; i < fileNames.length; i++) {
            if(filename.equals(fileNames[i])) {
                return i;
            }
        }

        return -1;
    }
}