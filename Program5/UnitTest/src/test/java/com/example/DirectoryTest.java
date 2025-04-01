package com.example;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for Directory.java, specifically testing the maximum number of files and max characters in file names.
 */
public class DirectoryTest {

    private Directory directory;
    private final int MAX_INUMBER = 100; // Define a reasonable max inode number for testing
    private final int MAX_CHARS = Constants.MAX_FILENAME_LENGTH;

    @BeforeEach
    void setUp() {
        directory = new Directory(MAX_INUMBER);
    }

    @AfterEach
    void tearDown() {
        directory = null;
    }

    @Test
    void testMaxFiles() {
        // Test maximum number of files
        for (int i = 1; i < MAX_INUMBER; i++) { // Start from 1 to avoid using inode 0 ("/")
            String filename = "file" + i;
            short inode = directory.ialloc(filename);
            assertNotEquals(-1, inode, "Failed to allocate inode for file: " + filename);
            assertEquals(i, inode, "Allocated inode number does not match expected.");
        }
        // Check if the next allocation fails due to lack of inodes
        assertThrows(AssertionError.class, () -> {
            String filename = "file" + MAX_INUMBER;
            short inode = directory.ialloc(filename);
            assertNotEquals(-1, inode);
        }, "Expected allocation to fail, but it did not.");
    }
    
    @Test
    void testMaxCharacters() {
        // Test maximum characters in file name
        String maxCharsFilename = "a".repeat(MAX_CHARS);
        short inodeMaxChars = directory.ialloc(maxCharsFilename);
        assertNotEquals(-1, inodeMaxChars, "Failed to allocate inode for file with max characters.");
        assertEquals(1, directory.namei(maxCharsFilename));

        // test characters greater than MAX_CHARS
        String longFileName = "b".repeat(MAX_CHARS+5);
        short inodeLongFileIndex = directory.ialloc(longFileName);
        assertNotEquals(-1, inodeLongFileIndex, "Failed to allocate inode for file with Long characters.");
        assertEquals(-1, directory.namei(longFileName));
    }

    @Test
    void testFreeingInodes() {
        // Allocate and then free some inodes
        short inode1 = directory.ialloc("file1");

        assertTrue(directory.ifree(inode1), "Failed to free inode: " + inode1);

        // Try to allocate a new inode and it should fill the free spot
        short inode4 = directory.ialloc("file4");
        assertEquals(inode1, inode4, "Reused inode number does not match expected.");
        
    }
    
     @Test
    void test_inode_number_associated_with_a_filename() {
        // Test retrieving inode number by filename
        String filename1 = "testfile1";
        short inode1 = directory.ialloc(filename1);
        assertNotEquals(-1, inode1, "Failed to allocate inode for " + filename1);
        assertEquals(inode1, directory.namei(filename1), "Inode number retrieval by name failed for " + filename1);

        String filename2 = "testfile2";
        directory.ialloc(filename2);
        assertNotEquals(-1,directory.namei(filename2), "Failed to allocate inode for " + filename2);
        assertEquals(-1, directory.namei("nonexistentfile"), "Found a non-existent file.");

        // Test the root inode
        assertEquals(0, directory.namei("/"), "Root inode number retrieval failed.");
    }


    @Test
    void testDirectoryToBytesAndBytesToDirectory() {
        // Allocate some files
        short inode1 = directory.ialloc("file1");
        short inode2 = directory.ialloc("file2");
        short inode3 = directory.ialloc("file3");

        // Convert to bytes
        byte[] directoryBytes = directory.directory2bytes();

        // Create a new directory object
        Directory newDirectory = new Directory(MAX_INUMBER);

        // Convert from bytes to directory
        newDirectory.bytes2directory(directoryBytes);

        // Assertions
        assertEquals(inode1, newDirectory.namei("file1"), "Directory to bytes/bytes to directory failed for file1.");
        assertEquals(inode2, newDirectory.namei("file2"), "Directory to bytes/bytes to directory failed for file2.");
        assertEquals(inode3, newDirectory.namei("file3"), "Directory to bytes/bytes to directory failed for file3.");
        assertEquals(0, newDirectory.namei("/"), "Root inode lost after directory conversion.");
    }
}