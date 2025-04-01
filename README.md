# operating-system
This project implements a Unix-like file system for the ThreadOS operating system simulator, enabling user programs to access persistent data on disk in a more manageable, stream-oriented way, rather than dealing with raw disk block access using rawread() and rawrite().

Assumptions and Limitations:
 * The system does not handle large file reads or writes efficiently due 
   to the limitations of its inode and block-based allocation model.
 * The inode structure, which is central to file management, restricts 
   file sizes to the sum of direct and indirect blocks, making it impractical 
   for very large files.
 * There is no caching mechanism implemented, which may result in slower 
   performance during repeated or concurrent file operations.
 * The file system does not support dynamic resizing, meaning it cannot 
   easily extend or adjust its block allocation size as needed.
