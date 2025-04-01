/**
 * File: PingPongDriver.java
 * Date: December 2024
 * Author: Stephen Dame
 * 
 * This class demonstrates the use of the PingPong class.
 * It creates two instances of PingPong with different arguments 
 * and starts their execution.
 * 
 * It is only meant to be run from the ThreadOS loader (e.g.):
 * -> l PingPongDriver
 */
public class PingPongDriver extends Thread {
 
    @Override
    public void run() {
        SysLib.cout("Starting PingPong Test..." + "\n");

        // Create and start the first PingPong instance 100ms
        String[] pingArgs = {"PingPong", "PING", "100"}; 
        SysLib.exec(pingArgs);

        // Create and start the second PingPong instance 50ms (2x faster)
        String[] pongArgs = {"PingPong", "pong", "50"}; 
        SysLib.exec(pongArgs);

        SysLib.join();          // wait for Ping Thread to complete
        SysLib.join();          // wait for Pong Thread to complete
        SysLib.exit();
        SysLib.cout("PingPong Test Complete!" + "\n");
    }
}