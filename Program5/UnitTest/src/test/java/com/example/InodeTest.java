package com.example;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Random;
import java.util.regex.Pattern;

public class InodeTest {

    private final PrintStream originalErr = System.err; // Capture the original System.err
    private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
    private final Random random = new Random();

    @BeforeEach
    void setUp() throws IOException {
        SysLib.initDisk();
        System.setErr(new PrintStream(outContent)); // Redirect System.err to outContent
    }

    @AfterEach
    void tearDown() {
        System.setErr(originalErr); // Restore System.err
        System.err.print(outContent.toString());
        System.err.flush();
        outContent.reset();
    }
    
    @Test
    public void testInodeConstants() {
        assertEquals(Constants.INODE_SIZE, Inode.iNodeSize);
        assertEquals(Constants.DIRECT_SIZE, Inode.directSize);
    }

    @Test
    void testInodeConstructor_NewInode() {
        Inode inode = new Inode();

        // Assert
        assertEquals(0, inode.length);
        assertEquals(0, inode.count);
        assertEquals(1, inode.flag);
        for (int i = 0; i < Inode.directSize; i++) {
            assertEquals(Inode.UNASSIGNED, inode.direct[i]);
        }
        assertEquals(Inode.UNASSIGNED, inode.indirect);
    }

    @Test
    public void testUnregisterIndexBlock_IndirectBlockAssigned() {
        // Arrange
        Inode inode = new Inode();
        short indirectBlockNumber = 10; // Example block number
        inode.indirect = indirectBlockNumber;

        // Initialize the index block with some data (not all -1)
        byte[] indexBlock = new byte[Disk.blockSize];
        for (int i = 0; i < Disk.blockSize / 2; i++) {
            if (i % 2 == 0) {
                SysLib.short2bytes((short) 100, indexBlock, i * 2);
            } else {
                SysLib.short2bytes((short) -1, indexBlock, i * 2);
            }
        }
        SysLib.rawwrite(indirectBlockNumber, indexBlock);

        // Act
        byte[] returnedData = inode.unregisterIndexBlock();

        // Assert
        assertNotNull(returnedData);
        assertEquals(Inode.UNASSIGNED, inode.indirect);
        // Verify Data
        for (int i = 0; i < Disk.blockSize / 2; i++) {
            if (i % 2 == 0) {
                assertEquals(100, SysLib.bytes2short(returnedData, i * 2));
            } else {
                assertEquals(-1, SysLib.bytes2short(returnedData, i * 2));
            }
        }
        // Verify block content on Disk
        byte[] blockContent = new byte[Disk.blockSize];
        SysLib.rawread(indirectBlockNumber, blockContent);
        for (int i = 0; i < Disk.blockSize; i++) {
            assertEquals(0, blockContent[i]);
        }
    }

    @Test
    public void testUnregisterIndexBlock_NoIndirectBlockAssigned() {
        // Arrange
        Inode inode = new Inode();

        // Act
        byte[] returnedData = inode.unregisterIndexBlock();

        // Assert
        assertNull(returnedData);
        assertEquals(Inode.UNASSIGNED, inode.indirect);
    }
    
        @Test
    public void testUnregisterIndexBlock_IndirectBlockAssigned_All_NegOne() {
        // Arrange
        Inode inode = new Inode();
        short indirectBlockNumber = 15; // Example block number
        inode.indirect = indirectBlockNumber;

        // Initialize the index block with all -1
        byte[] indexBlock = new byte[Disk.blockSize];
        for (int i = 0; i < Disk.blockSize / 2; i++) {
            SysLib.short2bytes((short) -1, indexBlock, i * 2);
        }
        SysLib.rawwrite(indirectBlockNumber, indexBlock);

        // Act
        byte[] returnedData = inode.unregisterIndexBlock();

        // Assert
        assertNotNull(returnedData);
        assertEquals(Inode.UNASSIGNED, inode.indirect);
        // Verify Data
        for (int i = 0; i < Disk.blockSize / 2; i++) {
            assertEquals(-1, SysLib.bytes2short(returnedData, i * 2));
        }
        // Verify block content on Disk
        byte[] blockContent = new byte[Disk.blockSize];
        SysLib.rawread(indirectBlockNumber, blockContent);
        for (int i = 0; i < Disk.blockSize; i++) {
            assertEquals(0, blockContent[i]);
        }
    }


    @Test
    void testInodeConstructor_FromDisk() {
        // Arrange
        short iNumber = 5; // Choose a free inode number
        Inode originalInode = new Inode();
        originalInode.length = 1024;
        originalInode.count = 3;
        originalInode.flag = 2;
        originalInode.direct[0] = 100;
        originalInode.direct[1] = 200;
        originalInode.indirect = 300;

        // Act
        originalInode.toDisk(iNumber); // Write the inode to disk
        Inode loadedInode = new Inode(iNumber); // Load it back from disk

        // Assert
        assertEquals(originalInode.length, loadedInode.length);
        assertEquals(originalInode.count, loadedInode.count);
        assertEquals(originalInode.flag, loadedInode.flag);
        for (int i = 0; i < Inode.directSize; i++) {
            assertEquals(originalInode.direct[i], loadedInode.direct[i]);
        }
        assertEquals(originalInode.indirect, loadedInode.indirect);
    }

    @Test
    void testToDisk() {
        // Arrange
        short iNumber = 6; // Choose a free inode number
        Inode originalInode = new Inode();
        originalInode.length = 2048;
        originalInode.count = 2;
        originalInode.flag = 3;
        originalInode.direct[0] = 101;
        originalInode.direct[1] = 201;
        originalInode.indirect = 301;

        // Act
        originalInode.toDisk(iNumber); // Write the inode to disk

        // Load the inode back from the disk
        Inode loadedInode = new Inode(iNumber);

        // Assert that the attributes are the same after writing and reading
        assertEquals(originalInode.length, loadedInode.length);
        assertEquals(originalInode.count, loadedInode.count);
        assertEquals(originalInode.flag, loadedInode.flag);
        for (int i = 0; i < Inode.directSize; i++) {
            assertEquals(originalInode.direct[i], loadedInode.direct[i]);
        }
        assertEquals(originalInode.indirect, loadedInode.indirect);

        // Modify the original inode
        originalInode.length = 4096;
        originalInode.count = 5;
        originalInode.flag = 4;
        originalInode.direct[0] = 102;
        originalInode.direct[1] = 202;
        originalInode.indirect = 302;

        // Write the modified inode back to disk
        originalInode.toDisk(iNumber);

        // Load it back from disk again
        Inode reloadedInode = new Inode(iNumber);

        // Assert that the attributes match the modified values
        assertEquals(originalInode.length, reloadedInode.length);
        assertEquals(originalInode.count, reloadedInode.count);
        assertEquals(originalInode.flag, reloadedInode.flag);
        for (int i = 0; i < Inode.directSize; i++) {
            assertEquals(originalInode.direct[i], reloadedInode.direct[i]);
        }
        assertEquals(originalInode.indirect, reloadedInode.indirect);
    }

    @Test
    void testFindTargetBlock() {
        Inode inode = new Inode();

        // Test Direct Blocks
        inode.direct[0] = 10;
        inode.direct[1] = 20;
        assertEquals(10, inode.findTargetBlock(0)); // Offset 0, should be block 10
        assertEquals(10, inode.findTargetBlock(Disk.blockSize - 1)); // Last byte of block 10
        assertEquals(20, inode.findTargetBlock(Disk.blockSize)); // First byte of block 20
        assertEquals(20, inode.findTargetBlock(2 * Disk.blockSize - 1)); // Last byte of block 20

        // Test Invalid direct block
        inode.direct[0] = Inode.UNASSIGNED;
        assertEquals(Inode.UNASSIGNED, inode.findTargetBlock(0));

        // Test Indirect Block
        short indirectBlockNumber = 30;
        short indirectTargetBlock = 40;
        inode.indirect = indirectBlockNumber;
        // Initialize the index block
        byte[] indexBlock = new byte[Disk.blockSize];
        SysLib.short2bytes(indirectTargetBlock, indexBlock, 0);
        SysLib.rawwrite(indirectBlockNumber, indexBlock);

        assertEquals(indirectTargetBlock, inode.findTargetBlock(Inode.directSize * Disk.blockSize)); // First indirect block
        assertEquals(indirectTargetBlock, inode.findTargetBlock((Inode.directSize + 1) * Disk.blockSize - 1)); // Last indirect block

        // Test invalid indirect block.
        inode.indirect = Inode.UNASSIGNED;
        assertEquals(Inode.UNASSIGNED, inode.findTargetBlock(Inode.directSize * Disk.blockSize));
         // test invalid indirect value.
        inode.indirect = indirectBlockNumber;
        SysLib.short2bytes(Inode.UNASSIGNED, indexBlock, 0);
        SysLib.rawwrite(indirectBlockNumber, indexBlock);
        assertEquals(Inode.UNASSIGNED, inode.findTargetBlock(Inode.directSize * Disk.blockSize));
    }

    @Test
    void testFindIndexBlock() {
        // Arrange
        Inode inode = new Inode();
        short testIndirectValue = 123;
        inode.indirect = testIndirectValue;

        // Act
        int returnedIndirect = inode.findIndexBlock();

        // Assert
        assertEquals(testIndirectValue, returnedIndirect);

        // Arrange
        inode.indirect = Inode.UNASSIGNED;
        // Act
        returnedIndirect = inode.findIndexBlock();
        // Assert
        assertEquals(Inode.UNASSIGNED, returnedIndirect);
    }

    @Test
    public void testPrintDefault() {
        // Arrange
        Inode inode = new Inode();

        // Act
        inode.printInode();
        String output = outContent.toString();

        // Assert
        assertTrue(output.contains("Inode Debug Information:"));
        assertTrue(output.contains("  Length: 0"));
        assertTrue(output.contains("  Count: 0"));
        assertTrue(output.contains("  Flag: 1"));
        assertTrue(output.contains("  Indirect Pointer: -1"));
        for (int i = 0; i < Inode.directSize; i++) {
            assertTrue(output.contains("    direct[" + i + "]: -1"));
        }
        assertTrue(output.contains("End Inode Debug Information."));
    }

    @Test
    public void testPrintWithData() {
        // Arrange
        Inode inode = new Inode();
        int length = random.nextInt(1000) + 1; // Length between 1 and 1000
        short count = (short) (random.nextInt(100) + 1); // Count between 1 and 100
        short flag = (short) (random.nextInt(5)); // Flag between 0 and 4
        short indirect = (short) (random.nextInt(1000) + 1); // Indirect block number between 1 and 1000

        inode.length = length;
        inode.count = count;
        inode.flag = flag;
        inode.indirect = indirect;
        // fill in direct array with random numbers.
        for (int i = 0; i < Inode.directSize; i++) {
            inode.direct[i] = (short) (random.nextInt(1000) + 1); // Each direct pointer between 1 and 1000
        }

        // Act
        inode.printInode();
        String output = outContent.toString();

        // Assert
        assertTrue(output.contains("Inode Debug Information:"));
        assertTrue(output.contains("  Length: " + length));
        assertTrue(output.contains("  Count: " + count));
        assertTrue(output.contains("  Flag: " + flag));
        assertTrue(output.contains("  Indirect Pointer: " + indirect));

        // Test direct array
        for (int i = 0; i < Inode.directSize; i++) {
            String pattern = "direct\\[" + i + "\\]: " + inode.direct[i];
            assertTrue(Pattern.compile(pattern).matcher(output).find());
        }
        assertTrue(output.contains("End Inode Debug Information."));
    }
}