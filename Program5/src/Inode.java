import java.util.Arrays;
/**
 * The Inode class represents an inode in a file system, 
 * which contains metadata about a file, including its size, 
 * number of references, and data block pointers.
 *
 * Overview:
 * - An inode is a data structure that stores essential information 
 *   about a file, such as its size, the number of references to it, 
 *   and its location on disk via direct and indirect pointers.
 * - Direct pointers in the inode reference actual data blocks, while 
 *   indirect pointers reference additional blocks that store more 
 *   data block addresses, supporting larger files.
 * - This class provides methods to manage inodes, read/write inode data 
 *   to disk, and interact with file system operations for block allocation.
 * 
 * Assumptions and Limitations:
 * - The inode class assumes that files can be split across multiple blocks, 
 *   using both direct and indirect block pointers for efficient storage.
 * - The system assumes a fixed inode size and block allocation size, 
 *   meaning scalability beyond these limits may not be handled efficiently.
 * - The class does not provide built-in protection against concurrent 
 *   access to inodes or advanced error recovery mechanisms.
 * 
 * Design:
 * - The inode structure is designed to handle file metadata and its 
 *   relationship with data blocks on disk, including direct and indirect 
 *   pointers for file storage.
 * - Key attributes include file size, reference count, and block pointers, 
 *   with support for registering and locating data blocks using indirect 
 *   indexing.
 * - The class provides low-level methods for interacting with disk data 
 *   and handles block allocation and deallocation for file storage.
 * 
 * Functionality and Performance Considerations:
 * - Inode operations like registering and finding target blocks are 
 *   optimized for efficient block lookups but may suffer from performance 
 *   degradation when handling large files due to reliance on indirect blocks.
 * - The class offers debugging methods (e.g., printInode()) to inspect 
 *   the structure and troubleshoot potential file system issues.
 * - Disk I/O operations, such as writing to disk and managing indirect 
 *   blocks, introduce latency when interacting with large files or 
 *   performing frequent inode updates.
 * - The class assumes a fixed block size and inode size, which may limit 
 *   the flexibility and scalability of the file system for larger or more 
 *   complex applications.
 *
 * Constants:
 * - iNodeSize: Size of an inode in bytes.
 * - directSize: Number of direct pointers in an inode.
 * - iNodesPerBlock: Number of inodes per disk block.
 * - Various error codes for error handling.
 *
 * Methods:
 * - Constructors to initialize inodes from scratch or disk.
 * - toDisk(): Writes inode data back to disk.
 * - registerIndexBlock(): Allocates an indirect block.
 * - unregisterIndexBlock(): Clears the indirect block.
 * - registerTargetBlock(): Assigns a new data block.
 * - findTargetBlock(): Retrieves the block for a file offset.
 * - printInode(): Prints inode structure for debugging.
 *
 * The Inode class works in conjunction with the SysLib and Disk classes 
 * to facilitate file system operations, including data block management 
 * and inode persistence on disk.
 *
 * By: Nolan Dela Rosa
 * 
 * March 20, 2025
 */

public class Inode {
    private SysLib sysLib;

    public final static int iNodeSize = Constants.INODE_SIZE;
    public final static int directSize = Constants.DIRECT_SIZE;
    public final static int iNodesPerBlock = Disk.blockSize / iNodeSize;

    public final static short NoError = 0;
    public final static short ErrorBlockRegistered = -1;
    public final static short ErrorPrecBlockUnused = -2;
    public final static short ErrorIndirectNull = -3;
    public final static short UNASSIGNED = -1;

    public int length; // file size in bytes
    public short count; // # file-table entries pointing to this
    public short flag; // 0 = unused, 1 = used, ...
    public short[] direct; // direct pointers
    public short indirect; // an indirect pointer

    public Inode() {
        length = 0;
        count = 0;
        flag = 1;
        direct = new short[directSize];
        for (int i = 0; i < directSize; i++) {
            direct[i] = UNASSIGNED;
        }
        indirect = UNASSIGNED;
    }

    /**
     * Inode constructor to create an Inode object from disk.
     * Reads the inode data from the disk based on the given iNumber.
     *
     * @param iNumber The inode number to retrieve from the disk.
     *                It's used to calculate the block number and offset where the
     *                inode data is stored.
     *
     *                The inode structure is laid out in the block as follows:
     * 
     *                offset + 0 (4 bytes): length - The file size in bytes.
     *                offset + 4 (2 bytes): count - The number of file-table
     *                entries pointing to this inode.
     *                offset + 6 (2 bytes): flag - The inode status flag.
     *                offset + 8 to offset + 31 (22 bytes): direct[0-10] - The
     *                direct pointers, each of which is a 2-byte short.
     *                offset + 30 (2 bytes): indirect - The indirect pointer.
     * 
     */
    public Inode(short iNumber) {
        int blockNumber = 1 + (iNumber / 16);
        int offset = (iNumber % 16) * iNodeSize;

        byte[] inodeData = new byte[Disk.blockSize];
        SysLib.rawread(blockNumber, inodeData);

        this.length = SysLib.bytes2int(inodeData, offset);
        this.count = SysLib.bytes2short(inodeData, offset + 4);
        this.flag = SysLib.bytes2short(inodeData, offset + 6);
        this.direct = new short[directSize];

        for(int i = 0; i < directSize; i++) {
            this.direct[i] = SysLib.bytes2short(inodeData, offset + 8 + i * 2);
        }

        this.indirect = SysLib.bytes2short(inodeData, offset + 30);
    }

    /**
     * Writes the current inode data to disk.
     * This method takes the current state of the inode (length, count, flag,
     * direct pointers, and indirect pointer) and writes it back to the
     * appropriate location on the disk based on the provided inode number.
     *
     * @param iNumber The inode number that determines where on the disk the
     *                inode data should be written.
     *
     *                The inode structure is laid out in the block as follows:
     * 
     *                offset + 0 (4 bytes): length - The file size in bytes
     *                offset + 4 (2 bytes): count - The number of file-table entries
     *                pointing to this inode
     *                offset + 6 (2 bytes): flag - The inode status flag
     *                offset + 8 to offset + 31 (22 bytes): direct[0-10] - The
     *                direct pointers, each of which is a 2-byte short.
     *                offset + 30 (2 bytes): indirect - The indirect pointer
     */
    public void toDisk(short iNumber) {
        int blockNumber = 1 + (iNumber / 16);
        int offset = (iNumber % 16) * iNodeSize;

        byte[] inodeData = new byte[Disk.blockSize];

        SysLib.int2bytes(length, inodeData, offset);
        SysLib.short2bytes(count, inodeData, offset + 4);
        SysLib.short2bytes(flag, inodeData, offset + 6);

        for(int i = 0; i < directSize; i++) {
            SysLib.short2bytes(this.direct[i], inodeData, offset + 8 + i * 2);
        }

        SysLib.short2bytes(indirect, inodeData, offset + 30);
        SysLib.rawwrite(blockNumber, inodeData);
    }

    /**
     * unregisterIndexBlock: unregister the IndexBlock and clear the content in disk
     *
     * @return the former content in the IndexBlock
     */
    public byte[] unregisterIndexBlock() {
        byte[] oldIndexBlock = new byte[Disk.blockSize];
        
        if(indirect != UNASSIGNED) {
            SysLib.rawread(indirect, oldIndexBlock);
            
            byte[] newIndexBlock = new byte[Disk.blockSize];
            Arrays.fill(newIndexBlock, (byte) 0);
            SysLib.rawwrite(indirect, newIndexBlock);

            indirect = Inode.UNASSIGNED;
            return oldIndexBlock;
        }

        return null;
    }

    /**
     * registerIndexBlock: Registers an indirect block for the inode.
     * This method assigns a given block number as the indirect block for the
     * inode, if all direct blocks have been assigned and the indirect block
     * is not already in use. It also initializes the newly assigned indirect
     * block with UNASSIGNED values.
     *
     * @param indexBlockNumber The block number to be registered as the indirect
     *                         block.
     * @return True if the indirect block was successfully registered, false
     *         otherwise.
     *         Conditions for failure:
     *         1. Not all direct pointers are assigned.
     *         2. The indirect pointer is already assigned.
     */
    boolean registerIndexBlock(short indexBlockNumber) {
        if(this.indirect != UNASSIGNED) {
            return false;
        }
        
        for(short d : this.direct) {
            if(d == UNASSIGNED) {
                return false;
            }
        }

        this.indirect = indexBlockNumber;
        return true;
    }

    /**
     * registerTargetBlock: register a new targetBlock
     *
     * @param offset            the offset of the target block
     * @param targetBlockNumber the index of new targetBlock
     * @return Inode.NoError when the targetBlock is registered
     */
    public short registerTargetBlock(int offset, short targetBlockNumber) {
        if(offset < directSize * Disk.blockSize) {
            int index = offset / Disk.blockSize;
            this.direct[index] = targetBlockNumber;

        } else {
            if(this.indirect == UNASSIGNED) {
                return ErrorIndirectNull;
            }

            byte[] indexBlock = new byte[Disk.blockSize];
            SysLib.rawread(this.indirect, indexBlock);
            int index = (offset - directSize * Disk.blockSize) / Disk.blockSize;
            SysLib.short2bytes(targetBlockNumber, indexBlock, index * 2);
            SysLib.rawwrite(this.indirect, indexBlock);
        }

        return NoError;
    }

    /**
     * findIndexBlock: find the IndexBlock
     *
     * @return the index of the IndexBlock
     */
    public int findIndexBlock() {
        return indirect;
    }

    /**
     * findTargetBlock: Finds the disk block number associated with a given file
     * offset.
     * This method determines whether the requested offset falls within the range of
     * direct pointers or requires accessing the indirect pointer. It then returns
     * the
     * corresponding disk block number where the data for that offset is stored.
     *
     * @param offset The byte offset within the file for which to find the
     *               corresponding
     *               disk block number.
     * @return The disk block number (a short) where the data for the given offset
     *         is
     *         stored. Returns UNASSIGNED if the offset is out of range or if the
     *         necessary pointers are unassigned.
     *         The logic is as follows:
     *         1. if offset < directSize * Disk.blockSize:
     *         The offset is within the range of direct pointers.
     *         Calculate the index of the direct pointer: offset / Disk.blockSize.
     *         If the direct pointer at the calculated index is valid (>= 0),
     *         return the block number stored in that direct pointer.
     *         2. else:
     *         The offset is beyond the range of direct pointers, so we need to use
     *         the
     *         indirect pointer.
     *         If the indirect pointer is valid (not UNASSIGNED):
     *         Read the indirect block from disk (newIndexBlock).
     *         Calculate the index within the indirect block: (offset - directSize *
     *         Disk.blockSize) / Disk.blockSize.
     *         This formula subtracts the size covered by direct pointers to get the
     *         relative offset within the indirect block's range.
     *         Retrieve the target block number from the indirect block using
     *         SysLib.bytes2short(newIndexBlock, index * 2).
     *         Each entry in the indirect block is a short (2 bytes), so we multiply
     *         the index by 2 to get the byte offset within the indirect block.
     *         Return the target block number.
     *         3. If the indirect pointer is UNASSIGNED or if the direct pointer
     *         is not valid, return UNASSIGNED.
     */
    public short findTargetBlock(int offset) {
        if(offset < directSize * Disk.blockSize) {
            int index = offset / Disk.blockSize;
            return this.direct[index];

        } else {
            if(this.indirect == UNASSIGNED) {
                return UNASSIGNED;
            }

            byte[] indexBlock = new byte[Disk.blockSize];
            SysLib.rawread(this.indirect, indexBlock);
            int index = (offset - directSize * Disk.blockSize) / Disk.blockSize;
            return SysLib.bytes2short(indexBlock, index * 2);
        }
    }

    /**
     * print: Print the entire inode structure
     */
    public void printInode() {
        SysLib.cerr("Inode Debug Information:\n");
        SysLib.cerr("  Length: " + length + "\n");
        SysLib.cerr("  Count: " + count + "\n");
        SysLib.cerr("  Flag: " + flag + "\n");

        SysLib.cerr("  Direct Pointers:\n");
        for (int i = 0; i < directSize; i++) {
            SysLib.cerr("    direct[" + i + "]: " + direct[i] + "\n");
        }

        SysLib.cerr("  Indirect Pointer: " + indirect + "\n");
        SysLib.cerr("End Inode Debug Information.\n");
    }
}