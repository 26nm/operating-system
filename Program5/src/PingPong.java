/**
* File: PingPong.java
* Date: December 2024
* Author: Stephen Dame (refactoring and documenting)
*
* Description: Runs a console print function thread that outputs a given string argment 100 times
* with a delay of a number of given milliseconds.
*
*
* This is the test program for CSS430 Homework 1B.
*/
public class PingPong extends Thread {
	private String word; 	 // a word to print out
	private int msec; 		 // number of milliseconds to wait
	private final int N=100; // Number of iterations

	/**
	 * This public method initializes a PingPong Thread
	 * 
	 * @param arg[0] - given string to print
	 * @param arg[1] - given milliseconds to wait between prints
	 */	
	public PingPong(String[] args) {
		word = args[0];
		msec = Integer.parseInt(args[1]);
	}

	/**
	 * The public standard Thread run method
	 */	
	public void run() {
		for (int j = 0; j < N; j++) {
			SysLib.cout(word + " ");
			SysLib.sleep(msec);
		}
		SysLib.cout("\n");
		SysLib.exit();
	}
}