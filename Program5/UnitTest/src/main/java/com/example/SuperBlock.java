package com.example;
/**
 * The SuperBlock class represents the metadata for the file system, stored at Block #0 on disk. 
 * It provides essential file system management functionalities such as tracking the total number of 
 * blocks, the number of inode blocks, and the free block list pointer. This class also offers 
 * mechanisms for formatting the file system, synchronizing superblock data with the disk, 
 * allocating and deallocating blocks, and validating the integrity of the file system.
 *
 * The SuperBlock contains the following key attributes:
 * - totalBlocks: The total number of blocks available on the disk.
 * - inodeBlocks: The number of blocks reserved for inodes.
 * - freeList: The index of the first available free block in the free block list.
 * 
 * Key methods:
 * - Constructor: Initializes the superblock from disk or formats the disk if needed.
 * - sync: Persists the current superblock data to disk, ensuring consistency across system restarts.
 * - format: Initializes the file system by resetting the superblock, setting up inodes, 
 *   and creating a free block list.
 * - getFreeBlock: Allocates and returns a free block from the free list.
 * - returnBlock: Returns a previously allocated block back to the free list.
 *
 * This class ensures that the file system is in a consistent state and manages free space 
 * efficiently through a linked list of free blocks. The superblock is a critical structure 
 * for disk management and ensures that file system operations can retrieve and free blocks as needed.
 * 
 * By: Nolan Dela Rosa
 * 
 * March 20, 2025
 */

 public class SuperBlock {
    private final int defaultInodeBlocks = Constants.DEFAULT_INODE_BLOCKS;
    public int totalBlocks;
    public int inodeBlocks;
    public int freeList;

    /**
     * Default constructor for the SuperBlock class.
     * 
     * This constructor initializes the SuperBlock with default values:
     * - Sets the total number of blocks on the disk using the constant TEST_DISK_SIZE from the Constants class.
     * - Sets the number of inode blocks to the default value defined in DEFAULT_INODE_BLOCKS from the Constants class.
     * - Calculates the free list starting block based on the number of inode blocks and the size of each inode (Inode.iNodeSize).
     * 
     * The constructor assumes that the disk size, inode size, and block size are predefined and uses those values to initialize
     * the SuperBlock for the disk.
     */
    public SuperBlock() {
       totalBlocks = Constants.TEST_DISK_BLOCKS;
       inodeBlocks = Constants.DEFAULT_INODE_BLOCKS;
       freeList = 1 + (this.inodeBlocks * Inode.iNodeSize) / Disk.blockSize;
    }

    /**
     * SuperBlock constructor.
     * Initializes the superblock by reading its data from the disk. If the disk's
     * contents are valid (i.e., the total number of blocks, inode blocks, and
     * the free list are consistent), it uses the data from the disk.
     * Otherwise, it performs a default format of the disk. The SuperBlock is
     * always located at Block #0.
     *
     * @param diskBlocks The total number of blocks in the disk.
     *                   This parameter is used to validate the disk's content
     *                   and to format the disk if necessary.
     */
    public SuperBlock(int diskBlocks) {
        byte[] superBlockData = new byte[Disk.blockSize];
        SysLib.rawread(0, superBlockData);
        totalBlocks = SysLib.bytes2int(superBlockData, 0); 
        inodeBlocks = SysLib.bytes2int(superBlockData, 4);
        freeList = SysLib.bytes2int(superBlockData, 8);
        freeList = 1 + (this.inodeBlocks * Inode.iNodeSize) / Disk.blockSize;
        
        if(totalBlocks == diskBlocks && inodeBlocks > 0 && freeList >= 1 + (inodeBlocks * Inode.iNodeSize) / Disk.blockSize) {
            return;

        } else {
            format();
        }
    }

    /**
     * Synchronizes the superblock data with the disk by writing its 
     * current state to block 0. This ensures that critical file system 
     * metadata (total blocks, inode blocks, and the free list pointer) 
     * is persisted across system restarts.
     * 
     * Steps:
     * 1. Create a byte array to hold the superblock data.
     * 2. Convert the total number of blocks, inode blocks, and the 
     *    free list pointer into bytes and store them in the array.
     * 3. Write the updated superblock data back to disk at block 0.
     * 
     * This method is crucial for maintaining consistency in the file 
     system, ensuring that any changes to the superblock are saved.
     */
    void sync() {
        byte[] superBlockData = new byte[Disk.blockSize];
        
        SysLib.int2bytes(totalBlocks, superBlockData, 0);
        SysLib.int2bytes(inodeBlocks, superBlockData, 4);
        SysLib.int2bytes(freeList, superBlockData, 8);

        SysLib.rawwrite(0, superBlockData);
    }

    /**
     * Formats the file system using the default number of inode blocks. 
     * This method calls the overloaded `format(int inodeBlocks)` method, 
     * passing in the predefined default inode block count.
     * 
     * Formatting initializes the superblock, sets up inodes, and 
     * prepares the free block list. It effectively resets the file system, 
     * erasing existing data and making the disk ready for new allocations.
     */
    void format() {
        format(defaultInodeBlocks);
    }

    /**
     * Formats the file system by initializing the superblock, setting up inodes, 
     * and preparing the free block list. This process erases any existing data 
     * and makes the disk ready for new allocations.
     * 
     * Steps:
     * 1. Set the total number of blocks from the disk size.
     * 2. Set the number of inode blocks based on the provided parameter.
     * 3. Set the free list to the first available block after the inodes.
     * 4. Initialize each inode and write it to disk.
     * 5. Initialize the free block list:
     *    - Each free block stores a reference to the next free block.
     *    - The last free block contains `-1` to indicate the end of the free list.
     * 6. Synchronize the superblock to persist these changes.
     * 
     * This method ensures that the file system starts in a clean state, 
     * with inodes properly set up and a linked list of free blocks ready for use.
     * 
     * @param numInodeBlocks The number of blocks reserved for inodes.
     */
    void format(int numInodeBlocks) {
        this.totalBlocks = Constants.TEST_DISK_BLOCKS;
        this.inodeBlocks = numInodeBlocks;
        this.freeList = 1 + (this.inodeBlocks * Inode.iNodeSize) / Disk.blockSize;

        for(int i = 0; i < inodeBlocks; i++) {
            Inode inode = new Inode();
            inode.toDisk((short)i);
        }

        for(int i = freeList; i < totalBlocks; i++) {
            byte[] block = new byte[Disk.blockSize];

            for(int j = 0; j < block.length; j++) {
               block[j] = 0; 
            }

            if(i < totalBlocks - 1) {
                SysLib.int2bytes(i + 1, block, 0);

            } else {
                SysLib.int2bytes(-1, block, 0);
            }

            SysLib.rawwrite(i, block);
        }

        sync();
    }

    /**
     * Retrieves a free block from the free list and updates the free list pointer. 
     * This method is responsible for allocating a new block for use in the file system.
     * 
     * Steps:
     * 1. Check if the free list is empty (i.e., `freeList < 0`). If so, return `-1` 
     *    to indicate that no free blocks are available.
     * 2. Store the current free block number.
     * 3. Read the block data from disk to retrieve the next free block reference.
     * 4. Update `freeList` to point to the next available free block.
     * 5. Return the allocated block number.
     * 
     * This method ensures that file system operations can obtain free blocks efficiently.
     * 
     * @return The block number of the newly allocated free block, or `-1` if none are available.
     */
    public int getFreeBlock() {
        if(freeList < 0 || freeList >= totalBlocks) {
            return -1;
        }
        // get a new free block from the freelist
        int freeBlockNumber = freeList;
        byte[] blockData = new byte[Disk.blockSize];
        SysLib.rawread(freeBlockNumber, blockData);
        freeList = SysLib.bytes2int(blockData, 0);

        return freeBlockNumber;
    }

    /**
     * Returns a previously allocated block to the free list, making it available 
     * for future use. This method effectively deallocates a block by linking it 
     * back into the free block list.
     * 
     * Steps:
     * 1. Validate `oldBlockNumber` to ensure it is within valid disk block range.
     *    - If the block number is invalid, return `false`.
     * 2. Store the current `freeList` pointer inside `oldBlockNumber` so that 
     *    this block becomes the new head of the free list.
     * 3. Write the modified block back to disk.
     * 4. Update `freeList` to point to `oldBlockNumber`, making it the new 
     *    starting point of the free list.
     * 5. Return `true` to indicate successful deallocation.
     * 
     * This method helps manage free space efficiently by maintaining a linked list 
     * of available blocks.
     * 
     * @param oldBlockNumber The block number to be returned to the free list.
     * @return `true` if the block was successfully returned, `false` if the block number is invalid.
     */
    public boolean returnBlock(int oldBlockNumber) {
        if(oldBlockNumber < 0 || oldBlockNumber >= totalBlocks) {
            return false;
        }

        byte[] blockData = new byte[Disk.blockSize];
        SysLib.int2bytes(freeList, blockData, 0);
        SysLib.rawwrite(oldBlockNumber, blockData);

        freeList = oldBlockNumber;
        return true;
    }
}