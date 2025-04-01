package com.example;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;

public class SuperBlockTest {

    private SuperBlock superBlock;

    @BeforeEach
    public void setUp() throws IOException {
        SysLib.initDisk(); // Reset the disk before each test
        superBlock = new SuperBlock(Constants.TEST_DISK_BLOCKS);
    }

    @Test
    void testSync() {
        // Act
        superBlock.sync(); // This now writes to the real disk
        // Assert
        SuperBlock reloadedSuperBlock = new SuperBlock(Constants.TEST_DISK_BLOCKS);
        assertEquals(superBlock.totalBlocks, reloadedSuperBlock.totalBlocks);
        assertEquals(superBlock.inodeBlocks, reloadedSuperBlock.inodeBlocks);
        assertEquals(superBlock.freeList, reloadedSuperBlock.freeList);
    }

    @Test
    void testDefaultConstructor() {
        // Arrange
        int diskSize = Constants.TEST_DISK_BLOCKS;
        int defaultInodeBlocks = 64;

        // Act
        SuperBlock defaultSuperBlock = new SuperBlock(diskSize);
        // Assert
        assertEquals(diskSize, defaultSuperBlock.totalBlocks);
        assertEquals(defaultInodeBlocks, defaultSuperBlock.inodeBlocks);
        assertEquals(1 + (defaultInodeBlocks * Inode.iNodeSize) / Disk.blockSize, defaultSuperBlock.freeList);
    }

    @Test
    void testFormat() {
        // Arrange
        int diskBlocks = Disk.blockSize;
        SuperBlock sb = new SuperBlock(Constants.DEFAULT_INODE_BLOCKS);
        // modify the super block
        sb.totalBlocks = Disk.blockSize;
        sb.inodeBlocks = Constants.DEFAULT_INODE_BLOCKS;
        sb.freeList = 200;

        // Act
        sb.format(Constants.DEFAULT_INODE_BLOCKS);
        sb.sync();
        SuperBlock reloadedSuperBlock = new SuperBlock(Constants.TEST_DISK_BLOCKS);
        // Assert
        assertEquals(Constants.TEST_DISK_BLOCKS, reloadedSuperBlock.totalBlocks);
        assertEquals(Constants.DEFAULT_INODE_BLOCKS, reloadedSuperBlock.inodeBlocks);
        assertEquals(1 + (Constants.DEFAULT_INODE_BLOCKS * Inode.iNodeSize) / Disk.blockSize, reloadedSuperBlock.freeList);
    }
}