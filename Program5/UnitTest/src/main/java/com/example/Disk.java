package com.example;

/**
 * File: Diskjava
 * Date: December 2024
 * Author: Stephen Dame
 * 
 * Refactoring of the original Disk.java to remove race
 * condition and clean up semantics around busy waiting.
 */
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.logging.StreamHandler;
import java.util.concurrent.CountDownLatch;

public class Disk extends Thread {

   private CountDownLatch syncLatch; // Add a CountDownLatch for the Sync method

   public synchronized CountDownLatch getSyncLatch() {
      return syncLatch;
   }

   // --- For debug logging only ---
   private static final Logger LOGGER = Logger.getLogger(Disk.class.getName());
   private static final boolean LOGGING_ENABLED = true;

   static {
      initializeLogger();
   }

   private static void initializeLogger() {
      LOGGER.setUseParentHandlers(false);
      StreamHandler handler = new StreamHandler(System.out, new MillisecondFormatter());
      LOGGER.addHandler(handler);
      LOGGER.setLevel(Level.INFO); // Set the logging level
   }

   private static class MillisecondFormatter extends Formatter {
      private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

      @Override
      public String format(LogRecord record) {
         Date date = new Date(record.getMillis());
         return String.format("%s %s: %s%n", dateFormat.format(date), record.getSourceMethodName(),
               record.getMessage());
      }
   }

   private void logMessage(Level level, String format, Object... params) {
      if (LOGGER.isLoggable(level)) {
         LOGGER.log(level, String.format(format, params));
      }
   }
   // --- For debug logging only ---

   public static final int blockSize = 512;
   private final int trackSize = 10;
   private final int transferTime = 20;
   private final int delayPerTrack = 1;

   public enum DiskState {
      IDLE, READ, WRITE, SYNC; // Correct and simplified enum definition
   }

   private DiskState diskState = DiskState.IDLE;

   /*
    * Public access to the Disk's object state
    */
   public DiskState getDiskState() {
      return diskState;
   }

   // Disk Object Instance Variables
   private int diskSize;
   private boolean diskBusy;
   private byte[] buffer;
   private byte[] data;
   private int currentBlockId;
   private int targetBlockId;

   private boolean started = false;
   private SysLib sysLib;

   public Disk(int totalBlocks, SysLib sysLib) {

      // --- For debug logging only ---
      if (LOGGING_ENABLED) {
         logMessage(Level.INFO, "Checking log Level INFO");
         logMessage(Level.FINE, "Checking log Level FINE");
      }
      // --- For debug logging only ---

      // Initialize all of the instance variables
      this.sysLib = sysLib;
      this.diskSize = (totalBlocks > 0) ? totalBlocks : 1;
      this.diskBusy = false;
      this.buffer = null;
      this.data = new byte[diskSize * blockSize];
      currentBlockId = 0;
      targetBlockId = 0;

      /*
       * First check to see if the threadOS DISK file (used to read and write byte
       * data)
       * exists.
       */
      try {
         File diskFile = new File("DISK");
         if (!diskFile.exists()) {
            this.sysLib.cerr("BOOT 2: DISK file created\n");
            // Create the file and initialize it with zeros
            try (FileOutputStream fos = new FileOutputStream(diskFile)) {
               fos.write(new byte[diskSize * blockSize]); // Initialize with zeros
            }
         }

         int fileSize = (int) diskFile.length();
         try (FileInputStream ifstream = new FileInputStream(diskFile)) {
            ifstream.read(this.data, 0, fileSize);
         }
      } catch (IOException e) {
         this.sysLib.cerr(e.toString() + "\n");
         // Handle the exception appropriately (e.g., throw a RuntimeException)
         throw new RuntimeException("Error initializing disk: " + e.getMessage());
      }
   }

   @Override
   public void start() {
      started = true; // Set the flag BEFORE calling super.start()
      super.start(); // This actually starts the thread execution
   }

   public synchronized boolean read(int blockId, byte buffer[]) {
      // --- For debug logging only ---
      // LOGGER.log(Level.FINE, "Thread {0} attempting to read block {1}. Disk State:
      // {2}",
      // new Object[] { Thread.currentThread().getId(), blockId, diskState });
      // --- For debug logging only ---

      if (buffer.length < blockSize) {
         throw new IllegalArgumentException(
               "Buffer size must be at least " + blockSize + " bytes for read operations.");
      }

      if (!started) {
         throw new IllegalStateException("READ: Disk not started. Call start() first.");
      }

      if (blockId < 0 || blockId > diskSize) {
         this.sysLib.cerr("threadOS: a wrong blockId for read\n");
         return false;
      }

      if (diskState == DiskState.IDLE && !this.diskBusy) {
         this.buffer = buffer;
         targetBlockId = blockId;
         diskState = DiskState.READ;
         this.diskBusy = true; // ensure busy now
         notify();
         return true;
      } else
         return false;
   }

   public synchronized boolean write(int blockId, byte buffer[]) {
      // --- For debug logging only ---
      // LOGGER.log(Level.INFO, "Thread {0} attempting to write block {1}. Disk State:
      // {2}",
      // new Object[] { Thread.currentThread().getId(), blockId, diskState });
      // --- For debug logging only ---

      if (buffer.length < blockSize) {
         throw new IllegalArgumentException(
               "Buffer size must be at least " + blockSize + " bytes for write operations.");
      }

      if (!started) {
         throw new IllegalStateException("WRITE: Disk not started. Call start() first.");
      }

      if (blockId < 0 || blockId > diskSize) {
         this.sysLib.cerr("threadOS: a wrong blockId for write\n");
         return false;
      }

      if (diskState == DiskState.IDLE && !this.diskBusy) {
         this.buffer = buffer;
         targetBlockId = blockId;
         diskState = DiskState.WRITE;
         this.diskBusy = true; // ensure busy now
         notify();
         return true;
      } else
         return false;
   }

   public synchronized boolean sync() {
      if (!started) {
         throw new IllegalStateException("SYNC: Disk not started. Call start() first.");
      }

      if (this.diskState == DiskState.IDLE && !this.diskBusy) {
         this.diskState = DiskState.SYNC;
         this.diskBusy = true; // ensure busy now
         syncLatch = new CountDownLatch(1);
         System.out.println("CountDownLatch initialized in sync()");
         notify();
         return true;
      } else {
         return false;
      }
   }

   /*
    * Simple polling of the Disk.isBusy() is a
    * getter operation not requiring synchronization
    */
   public boolean isBusy() {
      return this.diskBusy;
   }

   private void seek() {
      int seekTime = transferTime + delayPerTrack * Math.abs(targetBlockId / trackSize - currentBlockId / trackSize);

      try {
         Thread.sleep(seekTime);
      } catch (InterruptedException e) {
         this.sysLib.cerr(e.toString() + "\n");
      }

      // --- For debug logging only ---
      // String message = String.format("Thread %d seeking to block %d. Current block:
      // %d. Seek Time: %d",
      // Thread.currentThread().getId(), targetBlockId, currentBlockId, seekTime);

      // if (LOGGER.isLoggable(Level.FINE)) {
      // LOGGER.log(Level.FINE, message);
      // } else {
      // LOGGER.log(Level.INFO, "Seeking to block {0}. Current block: {1}",
      // targetBlockId, currentBlockId);
      // }
      // --- For debug logging only ---

      currentBlockId = targetBlockId;
   }


   private synchronized void finishDiskOperation() {
      // --- For debug logging only ---
      // LOGGER.log(Level.INFO, "Thread {0} Entering finishing operation. Disk State:
      // {1}",
      // new Object[] { Thread.currentThread().getId(), this.diskState });
      // --- For debug logging only ---
      this.diskState = DiskState.IDLE;
      this.diskBusy = false;
      notifyAll(); // Notify waiting threads
  }

   public void run() {
      try {
         while (!Thread.interrupted()) {
            synchronized (this) { // Synchronize on 'this'
               while (diskState == DiskState.IDLE) {
                  try {
                     wait();
                  } catch (InterruptedException e) {
                     Thread.currentThread().interrupt(); // Restore interrupt status
                     if (this.sysLib != null) {
                        this.sysLib.cerr("Disk thread interrupted: " + e.getMessage() + "\n");
                     }
                     return; // Exit the run method directly
                  }
               }
            } // End of synchronized block

            seek();
            switch (diskState) {
               case READ:
                  // --- For debug logging only ---
                  // LOGGER.log(Level.INFO, "READ operation: Copying from this.data to buffer");
                  // --- For debug logging only ---

                  System.arraycopy(this.data, targetBlockId * blockSize, buffer, 0, blockSize);
                  break;
               case WRITE:
                  // --- For debug logging only ---
                  // LOGGER.log(Level.INFO,"WRITE operation: Copying from buffer to this.data");
                  // --- For debug logging only ---
                  System.arraycopy(buffer, 0, this.data, targetBlockId * blockSize, buffer.length);
                  break;
               case SYNC:
                  // --- For debug logging only ---
                  // LOGGER.log(Level.INFO,"SYNC operation: Flushing this.data to DISK");
                  // --- For debug logging only ---
                  try {
                     FileOutputStream ofstream = new FileOutputStream("DISK");
                     ofstream.write(data);
                     ofstream.close();
                  } catch (FileNotFoundException e) {
                     this.sysLib.cerr(e.toString());
                  } catch (IOException e) {
                     this.sysLib.cerr(e.toString());
                  }
                  this.sysLib.cerr("threadOS: DISK synchronized\n");
                  System.out.println("CountDownLatch countDown() called in SYNC case");
                  syncLatch.countDown(); // Signal sync completion
                  break;
               default:
                  break;
            }
            finishDiskOperation();
         }
      } finally
      {
         if (this.sysLib != null) {
            this.sysLib.cerr("Disk thread exiting.\n");
         }
      }
   }

   /*
    * 
    * 
    * public void run() {
    * // --- For debug logging only ---
    * // LOGGER.log(Level.INFO, "Disk thread started.");
    * // --- For debug logging only ---
    * try {
    * while (true) {
    * try {
    * waitForDiskOperationCommand();
    * } catch (InterruptedException e) {
    * Thread.currentThread().interrupt(); // Restore interrupt status
    * if (this.sysLib != null) {
    * this.sysLib.cerr("Disk thread interrupted: " + e.getMessage() + "\n");
    * }
    * break; // Exit the loop on interrupt
    * }
    * seek();
    * 
    * switch (diskState) {
    * case READ:
    * 
    * // --- For debug logging only ---
    * // LOGGER.log(Level.INFO,
    * "READ operation: Copying from this.data to buffer");
    * // --- For debug logging only ---
    * 
    * System.arraycopy(this.data, targetBlockId * blockSize, buffer, 0, blockSize);
    * 
    * // Print the first 10 bytes (or less)
    * // System.out.print("Read from this.data into buffer (first 10 bytes)
    * this.data:
    * // ");
    * // for (int i = 0; i < Math.min(10, this.data.length); i++) {
    * // System.out.print(this.data[i] + " ");
    * // }
    * // System.out.println();
    * 
    * // // Print the first 10 bytes (or less)
    * // System.out.print("Read from this.data into buffer (first 10 bytes) buffer:
    * // ");
    * // for (int i = 0; i < Math.min(10, this.data.length); i++) {
    * // System.out.print(buffer[i] + " ");
    * // }
    * // System.out.println();
    * 
    * // --- For debug logging only ---
    * // Log data to be read
    * // StringBuilder sbRead = new StringBuilder("Read Data: ");
    * // for (int i = 0; i < 10 && i < buffer.length; i++) { // Log first 10 bytes
    * or
    * // less
    * // sbRead.append(buffer[i]).append(" ");
    * // }
    * // LOGGER.log(Level.INFO, sbRead.toString());
    * // --- For debug logging only ---
    * 
    * break;
    * case WRITE:
    * // --- For debug logging only ---
    * // LOGGER.log(Level.INFO,
    * "WRITE operation: Copying from buffer to this.data");
    * // --- For debug logging only ---
    * 
    * System.arraycopy(buffer, 0, this.data, targetBlockId * blockSize,
    * buffer.length);
    * 
    * // System.out.print("Write buffer into this.data (first 10 bytes) buffer: ");
    * // for (int i = 0; i < Math.min(10, this.data.length); i++) {
    * // System.out.print(buffer[i] + " ");
    * // }
    * // System.out.println();
    * 
    * //
    * System.out.print("Write buffer into this.data (first 10 bytes) this.data: ");
    * // for (int i = 0; i < Math.min(10, this.data.length); i++) {
    * // System.out.print(this.data[i] + " ");
    * // }
    * // System.out.println();
    * 
    * // --- For debug logging only ---
    * // Log data just written
    * // StringBuilder sbWrite = new StringBuilder("Written Data: ");
    * // for (int i = 0; i < 10 && i < this.data.length; i++) { // Log first 10
    * bytes
    * // or less
    * // sbWrite.append(this.data[targetBlockId * blockSize + i]).append(" ");
    * // }
    * // LOGGER.log(Level.INFO, sbWrite.toString());
    * // --- For debug logging only ---
    * 
    * break;
    * case SYNC:
    * try {
    * FileOutputStream ofstream = new FileOutputStream("DISK");
    * ofstream.write(data);
    * ofstream.close();
    * } catch (FileNotFoundException e) {
    * this.sysLib.cerr(e.toString());
    * } catch (IOException e) {
    * this.sysLib.cerr(e.toString());
    * }
    * this.sysLib.cerr("threadOS: DISK synchronized\n");
    * syncLatch.countDown(); // Signal sync completion
    * break;
    * default:
    * break;
    * }
    * 
    * // --- For debug logging only ---
    * // LOGGER.log(Level.INFO, "Disk State after switch: " + diskState); // Log
    * state
    * // after switch
    * // --- For debug logging only ---
    * 
    * finishDiskOperation();
    * }
    * } finally {
    * if (this.sysLib != null) {
    * this.sysLib.cerr("Disk thread exiting.\n");
    * }
    * }
    * }
    */

}