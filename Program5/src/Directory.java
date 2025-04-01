import java.util.Arrays;
/**
 * The Directory class represents a simple file directory structure 
 * within the file system. It is responsible for managing file names, 
 * their sizes, and inode allocations, enabling efficient file lookup 
 * and management.
 * 
 * Overview:
 * - The Directory class provides a fundamental mechanism for associating 
 *   file names with inode numbers in a flat directory structure.
 * - It maintains an internal mapping between file names and inodes 
 *   and provides serialization/deserialization methods to store 
 *   and retrieve directory data from disk.
 * - The root directory ("/") is always assigned the first inode.
 * 
 * Assumptions and Limitations:
 * - The directory structure is flat, meaning no support for hierarchical 
 *   directories or nested file organization.
 * - File names have a fixed maximum length (defined by Constants.MAX_FILENAME_LENGTH).
 * - If a filename exceeds the limit, it will be truncated.
 * - There is no built-in mechanism to handle filename duplicates, meaning 
 *   it is the responsibility of higher-level file system components to enforce uniqueness.
 * - The allocation strategy assumes that files are rarely deleted and added frequently, 
 *   which may lead to fragmentation over time.
 * 
 * Design:
 * - The directory maintains:
 *   - A boolean array (`inodeUsed[]`) to track allocated inodes.
 *   - An integer array (`fileNameSizes[]`) to store the lengths of file names.
 *   - A String array (`fileNames[]`) to store actual file names.
 * - Directory data is stored in a byte array for disk persistence, with 
 *   file name sizes stored first, followed by file names.
 * - The first inode is reserved for the root directory ("/").
 * 
 * Functionality and Performance Considerations:
 * - File lookup is performed through a linear search, making it inefficient 
 *   for large directories.
 * - Adding new files requires finding the first available inode, which 
 *   also involves scanning the `inodeUsed` array.
 * - Deleting files involves clearing their associated inode and metadata, 
 *   but does not compact or optimize space usage.
 * - Converting directory data to and from a byte array is relatively 
 *   lightweight but could be optimized with indexing techniques.
 * - The class assumes that higher-level components handle synchronization 
 *   and concurrency control, as it does not implement thread safety mechanisms.
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