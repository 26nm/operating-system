import java.util.*;

public class FileTable { // File Structure Table

	private Vector<FileTableEntry> table;// the entity of File Structure Table
	private Directory dir; // the root directory

	public FileTable(Directory directory) {// a default constructor
		table = new Vector<FileTableEntry>();// instantiate a file table
		dir = directory; // instantiate the root directory
	}

	public synchronized FileTableEntry falloc(String fname, String mode) {
		// allocate a new file table entry for this file name
		// allocate/retrieve and register the correspoding inode using dir
		// increment this inode's count
		// immediately write back this inode to the disk
		// return the entry index

		short iNumber;
		Inode inode = null;
		while (true) {
			if (fname.equals("/"))
				iNumber = 0; // the root directory's inode is 0
			else
				iNumber = dir.namei(fname);

			if (iNumber >= 0) { // file exists
				inode = new Inode(iNumber); // retrieve inode from disk
				if (mode.compareTo("r") == 0) {
					if (inode.flag == 0 || inode.flag == 1) {
						inode.flag = 1;
						break;
					}
					try {
						wait();
					} catch (InterruptedException e) {
					}
					;
				} else {
					if (inode.flag == 0 || inode.flag == 3) {
						inode.flag = 2;
						break;
					}
					if (inode.flag == 1 || inode.flag == 2) {
						inode.flag += 3;
						inode.toDisk(iNumber);
					}
					try {
						wait();
					} catch (InterruptedException e) {
					}
					;
				}
			} else {
				if (mode.compareTo("r") != 0) {
					iNumber = dir.ialloc(fname); // a new file
					inode = new Inode(); // create a new inode
					inode.flag = 2;
				} else
					return null;
				break;
			}
		}
		inode.count++; // a new FileTableEntry points to it
		inode.toDisk(iNumber); // reflect this inode to disk
		FileTableEntry e = new FileTableEntry(inode, iNumber, mode);
		table.addElement(e);
		return e; // return this new file table entry
	}

	public synchronized boolean ffree(FileTableEntry e) {
		// receive a file table entry
		// free the file table entry corresponding to this index
		if (table.removeElement(e) == true) { // find this file table entry
			e.inode.count--; // this entry no longer points to this inode
			switch (e.inode.flag) {
				case 1:
					e.inode.flag = 0;
					break;
				case 2:
					e.inode.flag = 0;
					break;
				case 4:
					e.inode.flag = 3;
					break;
				case 5:
					e.inode.flag = 3;
					break;
			}
			e.inode.toDisk(e.iNumber); // reflect this inode to disk
			e = null; // this file table entry is erased.
			notify();
			return true;
		} else
			return false;
	}

	public synchronized boolean fempty() {
		return table.isEmpty(); // return if table is empty
	} // called before a format
}
