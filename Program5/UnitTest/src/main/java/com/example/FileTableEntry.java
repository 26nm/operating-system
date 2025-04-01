package com.example;

public class FileTableEntry {
    public int seekPtr; // a file pointer
    public final Inode inode;
    public final short iNumber;
    public final String mode;
    public int count; // # threads

    /**
     * FileTableEntry constructor
     * 
     * @param i    inode of this file
     * @param inum iNumber of this file
     * @param m    mode of this file
     */
    public FileTableEntry(Inode i, short inum, String m) {
        seekPtr = 0; // default seek pointer
        inode = i; // inode of the file
        iNumber = inum; // iNumber of the file
        mode = m; // mode of the file
        count = 1; // at least on thread is using it
    }
}