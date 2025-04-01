----------------------------------
Unix-like File System for ThreadOS
----------------------------------
This project implements a Unix-like file system for the ThreadOS operating system simulator, enabling user programs to access persistent data on disk in a more manageable, stream-oriented way, rather than dealing with raw disk block access using rawread() and rawrite().

------------
Core Classes
------------
The project includes the following four core classes:

FileSystem: Manages file storage, retrieval, and metadata organization. It provides functionality for file operations like opening, reading, writing, and closing files. It also handles file synchronization and storage block allocation.

Directory: Simulates a directory structure, mapping file names to inode numbers. It handles file lookup, inode allocation, and deallocation, as well as storing and retrieving directory information from disk.

SuperBlock: Represents the metadata of the file system stored at Block #0 on disk. It tracks the total number of blocks, inode blocks, and free block list pointer. The SuperBlock ensures the consistency and integrity of the file system by managing block allocation and deallocation.

Inode: Represents an inode in the file system, containing metadata such as file size, reference count, and pointers to data blocks. It manages direct and indirect block allocations and provides methods for data block registration and retrieval.

For more details about each class, please refer to their respective files in the project.