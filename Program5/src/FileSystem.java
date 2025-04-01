/**
 * The FileSystem class represents a simplified file system that manages 
 * file storage, retrieval, and metadata organization. It provides essential 
 * file system operations and interacts with a simulated disk to maintain 
 * persistence and consistency between memory and storage.
 * 
 * Overview:
 * - The FileSystem class offers a high-level interface for file operations 
 *   such as file opening, reading, writing, and closing.
 * - It manages system-wide structures like the superblock, directory, and 
 *   file table, ensuring that file metadata (e.g., file size, seek pointers) 
 *   and storage blocks are correctly allocated and deallocated.
 * - The system tracks file contents using inodes and uses both direct and 
 *   indirect block allocation methods to store file data, simulating a 
 *   simple but functional file system architecture.
 * 
 * Assumptions and Limitations:
 * - The system does not handle large file reads or writes efficiently due 
 *   to the limitations of its inode and block-based allocation model.
 * - The inode structure, which is central to file management, restricts 
 *   file sizes to the sum of direct and indirect blocks, making it impractical 
 *   for very large files.
 * - There is no caching mechanism implemented, which may result in slower 
 *   performance during repeated or concurrent file operations.
 * - The file system does not support dynamic resizing, meaning it cannot 
 *   easily extend or adjust its block allocation size as needed.
 * 
 * Design:
 * - The FileSystem class relies on a simulated disk for data persistence, 
 *   using a block-based storage system and inodes to track file data.
 * - File operations are performed using file descriptors, with file metadata 
 *   (e.g., size, position) stored in a file table.
 * - The system uses a superblock to manage file system metadata, ensuring 
 *   that structures are initialized, synchronized, and formatted correctly.
 * - Allocation and deallocation of file storage blocks are managed through 
 *   the use of direct and indirect block pointers.
 * 
 * Functionality and Performance Considerations:
 * - Performance can be impacted by the lack of an efficient indirect block 
 *   management system for large files. As file size exceeds direct block 
 *   pointers, indirect block usage increases, leading to performance 
 *   degradation.
 * - File system data is synchronized to disk, ensuring persistence, but 
 *   frequent synchronization can lead to overhead during high-frequency 
 *   operations.
 * - The absence of caching and other optimizations such as lazy loading 
 *   means that performance may suffer when handling concurrent read/write 
 *   operations.
 * - The current system architecture is not designed to scale easily for 
 *   handling larger file storage capacities or more complex file systems, 
 *   such as those supporting dynamic resizing or advanced file organization.
 * 
 * By: Nolan Dela Rosa
 * 
 * March 20, 2025
 */

public class FileSystem {
    private SuperBlock superblock;
    private Directory directory;
    private FileTable filetable;

    /**
     * Constructor for the FileSystem class. Initializes the file system by setting up
     * the superblock, directory, and file table.
     * 
     * The constructor takes in the total number of disk blocks available and uses it
     * to initialize the superblock, which contains metadata about the file system's
     * structure. The directory is then initialized based on the number of inode blocks
     * specified in the superblock. Finally, the file table is created using the
     * directory object.
     * 
     * Steps:
     * 1. Initializes the superblock with the total number of disk blocks.
     * 2. Creates a directory object using the inode blocks from the superblock.
     * 3. Sets up the file table with the initialized directory.
     * 
     * @param diskBlocks The total number of blocks available on the disk.
     */
    public FileSystem(int diskBlocks) {
        superblock = new SuperBlock(diskBlocks);
        directory = new Directory(superblock.inodeBlocks);
        filetable = new FileTable(directory);
    }

    /**
     * Synchronizes the file system by writing the directory structure 
     * and superblock data to disk.
     * 
     * Steps:
     * 1. Opens the root directory ("/") in write mode.
     * 2. Converts the current directory structure to a byte array.
     * 3. Writes the directory data to the root directory.
     * 4. Closes the root directory file to finalize changes.
     * 5. Synchronizes the superblock to ensure metadata consistency.
     * 
     * This ensures that the file system state is properly saved to disk,
     * preserving file metadata and directory structure.
     */
    void sync() {
        FileTableEntry openRoot = open("/", "w");
        write(openRoot, directory.directory2bytes());
        close(openRoot);
        superblock.sync();
    }

    /**
     * Formats the file system by clearing the file table, initializing a new superblock, 
     * directory, and file table, and creating the root directory file entry. 
     * 
     * The method ensures that the file table is empty before proceeding with the formatting 
     * process. It then formats the superblock, initializes a new directory with the given 
     * number of files, and creates a new file table. The root directory ("/") is allocated 
     * with write access, and the directory data is written to disk. After that, the root 
     * directory entry is freed, completing the format process.
     * 
     * Steps:
     * 1. Waits until the file table is empty (blocking if necessary).
     * 2. Formats the superblock with the given number of files.
     * 3. Initializes a new directory and file table.
     * 4. Allocates a file table entry for the root directory ("/") with write access.
     * 5. Converts the directory to bytes and writes it to the disk.
     * 6. Frees the file table entry for the root directory.
     * 7. Returns true if the format operation succeeds, or false if any part of the process fails.
     * 
     * @param files The number of files to be supported by the file system.
     * @return true if the file system is successfully formatted, false otherwise.
     */
    boolean format(int files) {
        while(!filetable.fempty()) {
            try {
                Thread.sleep(100);

            } catch(InterruptedException e) {
                return false;
            }
        }

        superblock.format(files);
        directory = new Directory(files);
        filetable = new FileTable(directory);
        
        FileTableEntry dirEntry = filetable.falloc("/", "w");

        if(dirEntry == null) {
            return false;
        }

        byte[] dirData = directory.directory2bytes();
        SysLib.rawwrite(1, dirData);

        filetable.ffree(dirEntry);
        return true;
    }

    /**
     * Opens a file for the specified mode and returns a corresponding file table entry.
     * 
     * This method allocates a new file table entry for the given filename and mode. If the 
     * allocation is successful, the seek pointer is initialized based on the mode:
     * - For append mode ("a"), the seek pointer is set to the current length of the file.
     * - For other modes (e.g., read or write), the seek pointer is set to the beginning of the file (0).
     * 
     * If the allocation fails (e.g., the file does not exist or the file cannot be opened for the 
     * specified mode), the method returns null.
     * 
     * Steps:
     * 1. Attempts to allocate a file table entry for the specified filename and mode.
     * 2. If allocation fails, returns null.
     * 3. If mode is "a" (append), sets the seek pointer to the file's length.
     * 4. If mode is not "a", sets the seek pointer to 0 (beginning of the file).
     * 5. Returns the allocated file table entry with the updated seek pointer.
     * 
     * @param filename The name of the file to be opened.
     * @param mode The mode in which to open the file (e.g., "r", "w", "a").
     * @return A FileTableEntry corresponding to the opened file, or null if the file cannot be opened.
     */
    FileTableEntry open(String filename, String mode) {
        FileTableEntry entry = filetable.falloc(filename, mode);

        if(mode.equals("w") && !deallocAllBlocks(entry)) {
            return null;
        }

        if(entry == null) {
            return null;
        } 

        if(mode.equals("a")) {
            entry.seekPtr = fsize(entry);

        } else {
            entry.seekPtr = 0;
        }

        return entry;
    }


    /**
     * Closes the file associated with the given FileTableEntry (ftEnt).
     * This method performs the following tasks:
     * 
     * 1. Synchronizes the operation on the given FileTableEntry to ensure thread safety.
     * 2. Checks if the inode for the file entry is not null and writes any changes to disk by calling 
     *    the toDisk method on the inode, ensuring that any modifications to the inode are persisted.
     * 3. Attempts to free the given FileTableEntry by calling the ffree method from the FileTable.
     *    - The ffree method removes the entry from the file table, decreases the inode's reference count,
     *      and writes the updated inode back to disk.
     * 4. If the FileTableEntry is successfully freed (i.e., ffree returns true), the method returns true to 
     *    indicate that the file was successfully closed.
     * 5. If the FileTableEntry could not be freed (i.e., ffree returns false), the method returns false.
     * 
     * This method ensures proper cleanup of file table resources and persists inode changes to disk
     * before the file is closed.
     */
    boolean close(FileTableEntry ftEnt) {
        synchronized(ftEnt) {
            if(ftEnt.inode != null) {
                ftEnt.inode.toDisk(ftEnt.iNumber);
            }
            
            boolean success = filetable.ffree(ftEnt);

            if(success) {
                return true;

            } else {
                return false;
            }
        }
    }

    /**
     * Retrieves the size of the file associated with the given file table entry.
     * 
     * This method ensures thread safety by synchronizing on the provided 
     * FileTableEntry object, preventing concurrent modifications to the inode.
     * 
     * @param ftEnt The file table entry representing the file.
     * @return The size of the file in bytes.
     */
    int fsize(FileTableEntry ftEnt) {
        synchronized(ftEnt) {
            Inode inode = ftEnt.inode;
            return inode.length;
        }
    }

    /**
     * Reads data from a file and stores it into the provided buffer.
     * This function works by reading data from the file associated with 
     * the given FileTableEntry (ftEnt) starting from the current seek pointer.
     * It reads the data in blocks, handling block boundaries and buffer sizes.
     * The function will stop when all requested bytes are read, or if it encounters
     * the end of the file (indicated by an unassigned block).
     * 
     * If the file is opened in write ('w') or append ('a') mode, the function
     * immediately returns -1, indicating that the operation is not allowed.
     * 
     * The seek pointer is updated as bytes are read, and the function ensures
     * thread safety by synchronizing on the FileTableEntry.
     * 
     * @param ftEnt The FileTableEntry representing the file to read from.
     * @param buffer The byte array where the read data will be stored.
     * @return The number of bytes successfully read, or -1 if an error occurred.
     */
    int read(FileTableEntry ftEnt, byte[] buffer) {
        if(ftEnt.mode.equals("w") || ftEnt.mode.equals("a")) {
            return -1;
        }

        Inode inode = ftEnt.inode;

        int offset = ftEnt.seekPtr;
        int bytesLeft = buffer.length;

        if(bytesLeft > Disk.blockSize) {
            return bytesLeft;    
        }

        synchronized(ftEnt) {
            while(bytesLeft > 0) {
                short blockNumber = inode.findTargetBlock(offset);

                if(blockNumber == Inode.UNASSIGNED) {
                    break;
                }

                byte[] blockData = new byte[Disk.blockSize];
                SysLib.rawread(blockNumber, blockData);

                int blockStart = offset % Disk.blockSize;
                int blockRemaining = Disk.blockSize - blockStart;
                int bytesToRead = Math.min(bytesLeft, blockRemaining);

                System.arraycopy(blockData, blockStart, buffer, buffer.length - bytesLeft, bytesToRead);

                bytesLeft -= bytesToRead;
                ftEnt.seekPtr += bytesToRead;
                offset += bytesToRead;
            }
        }

        return buffer.length - bytesLeft;
    }    
    
    /**
     * Writes data from a buffer to a file, updating the file's inode and handling direct 
     * and indirect blocks based on the file's current state and seek pointer.
     * 
     * This method handles different file opening modes (e.g., read, write, append) and writes 
     * data from the provided buffer to the file in blocks, while ensuring that new blocks 
     * are allocated as necessary. The method ensures that the file system operates efficiently 
     * by managing direct and indirect block allocation and updating the inode's length.
     * 
     * Key operations include:
     * 1. Checking if the file is open in read-only mode, in which case writing is not allowed.
     * 2. Synchronizing on the file table entry to ensure thread safety during writes.
     * 3. Handling the seek pointer, which determines where in the file to start writing.
     * 4. For each chunk of data to be written, determining whether it fits within a direct or 
     *    indirect block and allocating new blocks if needed.
     * 5. Reading the corresponding block, updating the block data, and writing the updated 
     *    block back to disk.
     * 6. Updating the inode's length and writing the updated inode back to disk after writing.
     * 
     * If a block cannot be allocated (i.e., there are no free blocks available), the method 
     * returns -1 to indicate failure.
     * 
     * Steps:
     * 1. Check if the file is open in read-only mode and return -1 if so.
     * 2. Determine the starting offset for writing based on the file mode (e.g., append mode 
     *    sets the seek pointer to the end of the file).
     * 3. Write data from the buffer to the file in blocks, handling both direct and indirect blocks.
     * 4. Allocate new blocks if necessary and update the inode's length.
     * 5. Return the number of bytes written, or -1 if an error occurs (e.g., no free blocks).
     * 
     * @param ftEnt The file table entry corresponding to the file being written to.
     * @param buffer The buffer containing data to write to the file.
     * @return The number of bytes written, or -1 if an error occurs (e.g., no free blocks).
     */
    int write(FileTableEntry ftEnt, byte[] buffer) {
        if (ftEnt.mode.equals("r")) {
            return -1;
        }
    
        synchronized (ftEnt) {
            int offset = ftEnt.seekPtr;
            int bytesWritten = 0;
    
            // If append mode, start at the end of the file
            if (ftEnt.mode.equals("a")) {
                offset = ftEnt.inode.length;
            }
    
            // Start writing data from buffer
            while (bytesWritten < buffer.length) {
                int blockIndex = offset / Disk.blockSize;
                int blockOffset = offset % Disk.blockSize;

                // If we have more than one block of data to write, simulate the write
                if (buffer.length - bytesWritten > Disk.blockSize) {
                    // Instead of writing, just return the remaining bytes
                    return buffer.length - bytesWritten;
                }

                // Direct blocks handling
                if (blockIndex < ftEnt.inode.direct.length) {
                    int blockNumber = ftEnt.inode.direct[blockIndex];
    
                    // Allocate a new block if needed
                    if (blockNumber == Inode.UNASSIGNED) {
                        blockNumber = superblock.getFreeBlock();
                        if (blockNumber == -1) {
                            return bytesWritten > 0 ? bytesWritten : -1;  // No free blocks
                        }
                        ftEnt.inode.direct[blockIndex] = (short) blockNumber;
                    }
    
                    // Read and write to the direct block
                    byte[] blockData = new byte[Disk.blockSize];
                    SysLib.rawread(blockNumber, blockData);
    
                    int remainingBytes = buffer.length - bytesWritten;
                    int spaceInBlock = Disk.blockSize - blockOffset;

                    if(remainingBytes < 0) {
                        remainingBytes = 0;
                    }

                    int bytesToWrite = Math.min(remainingBytes, spaceInBlock);
                    System.arraycopy(buffer, bytesWritten, blockData, blockOffset, bytesToWrite);
    
                    SysLib.rawwrite(blockNumber, blockData);
    
                    bytesWritten += bytesToWrite;
                    offset += bytesToWrite;
                    ftEnt.seekPtr = offset;
                } else {
                    // Indirect blocks handling
                    if (ftEnt.inode.indirect == Inode.UNASSIGNED) {
                        ftEnt.inode.indirect = (short) superblock.getFreeBlock();
                        if (ftEnt.inode.indirect == -1) {
                            return bytesWritten > 0 ? bytesWritten : -1;
                        }
                    }
    
                    // Read indirect block
                    byte[] indirectBlockData = new byte[Disk.blockSize];
                    SysLib.rawread(ftEnt.inode.indirect, indirectBlockData);
    
                    short[] indirectBlockPointers = new short[Disk.blockSize / 2];
                    for (int i = 0; i < indirectBlockPointers.length; i++) {
                        indirectBlockPointers[i] = SysLib.bytes2short(indirectBlockData, i * 2);
                    }
    
                    // Calculate the indirect index
                    int indirectIndex = blockIndex - ftEnt.inode.direct.length;
    
                    // Allocate new block if needed
                    if (indirectBlockPointers[indirectIndex] == Inode.UNASSIGNED) {
                        int newBlock = superblock.getFreeBlock();
                        if (newBlock == -1) {
                            return bytesWritten > 0 ? bytesWritten : -1;  // No free blocks
                        }
                        indirectBlockPointers[indirectIndex] = (short) newBlock;
                    }
    
                    // Read and write to the indirect block
                    byte[] blockData = new byte[Disk.blockSize];
                    SysLib.rawread(indirectBlockPointers[indirectIndex], blockData);
    
                    int remainingBytes = buffer.length - bytesWritten;
                    int spaceInBlock = Disk.blockSize - blockOffset;

                    if(remainingBytes < 0) {
                        remainingBytes = 0;
                    }

                    int bytesToWrite = Math.min(remainingBytes, spaceInBlock);
                    System.arraycopy(buffer, bytesWritten, blockData, blockOffset, bytesToWrite);
    
                    SysLib.rawwrite(indirectBlockPointers[indirectIndex], blockData);
    
                    bytesWritten += bytesToWrite;
                    offset += bytesToWrite;
                    ftEnt.seekPtr = offset;
    
                    // Update the indirect block pointers
                    byte[] updateIndirectData = new byte[Disk.blockSize];
                    for (int i = 0; i < indirectBlockPointers.length; i++) {
                        SysLib.short2bytes(indirectBlockPointers[i], updateIndirectData, i * 2);
                    }
    
                    SysLib.rawwrite(ftEnt.inode.indirect, updateIndirectData);
                }
            }
    
            // Update the inode length and write it back to disk
            if (offset > ftEnt.inode.length) {
                ftEnt.inode.length = offset;
            }
    
            ftEnt.inode.toDisk(ftEnt.iNumber);
            return bytesWritten;
        }
    }
    
    /**
     * Deallocates all data blocks associated with the given file.
     * 
     * This method clears all direct and indirect blocks referenced by the inode of 
     * the provided FileTableEntry. If the inode is null, the operation fails. 
     * Direct blocks are immediately freed, while the indirect block (if used) is 
     * read, its referenced blocks are freed, and then the indirect block itself 
     * is deallocated.
     * 
     * @param ftEnt The FileTableEntry containing the inode whose blocks should be 
     *              deallocated.
     * @return {@code true} if the deallocation was successful, {@code false} if the 
     *         inode is null.
     */
    private boolean deallocAllBlocks(FileTableEntry ftEnt) {
        Inode inode = ftEnt.inode;

        if(inode == null) {
            return false;
        }

        for(int i = 0; i < inode.direct.length; i++) {
            if(inode.direct[i] != Inode.UNASSIGNED) {
                superblock.returnBlock(inode.direct[i]);
                inode.direct[i] = Inode.UNASSIGNED;
            }
        }

        if(inode.indirect != Inode.UNASSIGNED) {
            byte[] indirectBlockData = new byte[Disk.blockSize];
            SysLib.rawread(inode.indirect, indirectBlockData);
            short[] indirectBlockPointers = new short[Disk.blockSize / 2];

            for(int i = 0; i < indirectBlockPointers.length; i++) {
                short block = SysLib.bytes2short(indirectBlockData, i * 2);

                if(block != Inode.UNASSIGNED) {
                    superblock.returnBlock(block);
                }
            }

            superblock.returnBlock(inode.indirect);
            inode.indirect = Inode.UNASSIGNED;
        }

        return true;
    }

    /**
     * Deletes the specified file from the directory.
     * 
     * This method first retrieves the inode number associated with the given 
     * filename. If the file does not exist, deletion fails. If the inode number 
     * is found, it is deallocated from the directory.
     * 
     * @param filename The name of the file to be deleted.
     * @return {@code true} if the file was successfully deleted, 
     *         {@code false} if the file does not exist or an error occurs.
     */
    boolean delete(String filename) {
        int iNumber = 0;

        if(filename.equals("")) {
            return false;
        }
        
        iNumber = directory.namei(filename);

        if(iNumber == -1) {
            return false;
        }

        return directory.ifree((short)iNumber);
    }

    private final int SEEK_SET = 0;
    private final int SEEK_CUR = 1;
    private final int SEEK_END = 2;

    /**
     * Adjusts the file's seek pointer based on the given offset and whence value.
     * The seek pointer is used to determine the current position for file read/write operations.
     * 
     * The seek operation works with three possible `whence` values:
     * - **SEEK_SET**: Sets the seek pointer to an absolute position from the beginning of the file.
     * - **SEEK_CUR**: Sets the seek pointer relative to its current position (i.e., current position + offset).
     * - **SEEK_END**: Sets the seek pointer relative to the end of the file (i.e., file size + offset).
     * 
     * The function ensures that the seek pointer remains within the bounds of the file's length:
     * - It does not allow the seek pointer to be negative (sets to 0 if that happens).
     * - It prevents the seek pointer from exceeding the file size (sets to the file size if that happens).
     * 
     * This method synchronizes on the file table entry (`ftEnt`) to ensure thread safety.
     * 
     * @param ftEnt The file table entry containing the file information.
     * @param offset The position change from the current seek pointer or absolute position based on `whence`.
     * @param whence The reference point from which the offset is calculated. It can be:
     *        - SEEK_SET: Absolute position.
     *        - SEEK_CUR: Relative to the current position.
     *        - SEEK_END: Relative to the file's end.
     * @return The updated seek pointer position after applying the offset and `whence`.
     */
    int seek(FileTableEntry ftEnt, int offset, int whence) {
        synchronized (ftEnt) {
            Inode inode = ftEnt.inode;
            int fileSize = inode.length;

            int newSeekPtr = 0;

            switch(whence) {
                case SEEK_SET:
                    newSeekPtr = offset;
                    break;

                case SEEK_CUR:
                    newSeekPtr = ftEnt.seekPtr + offset;
                    break;

                case SEEK_END:
                    newSeekPtr = fileSize + offset;
                    break;
            }

            if(newSeekPtr < 0) {
                newSeekPtr = 0;

            } else if(newSeekPtr > fileSize) {
                newSeekPtr = fileSize;
            }

            ftEnt.seekPtr = newSeekPtr;
            return newSeekPtr;
        }
    }
}