/*
 * Copyright (c) 2002-@year@, University of Maryland
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided
 * that the following conditions are met:
 *
 * Redistributions of source code must retain the above copyright notice, this list of conditions
 * and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice, this list of conditions
 * and the following disclaimer in the documentation and/or other materials provided with the
 * distribution.
 *
 * Neither the name of the University of Maryland nor the names of its contributors may be used to
 * endorse or promote products derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
 * PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR
 * TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * Piccolo was written at the Human-Computer Interaction Laboratory www.cs.umd.edu/hcil by Jesse Grosjean
 * under the supervision of Ben Bederson. The Piccolo website is www.cs.umd.edu/hcil/piccolo.
 */
package edu.umd.cs.piccolo.util;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;

import javax.swing.SwingUtilities;

/**
 * <b>PDebug</b> is used to set framework wide debugging flags.
 * <P>
 * @version 1.0
 * @author Jesse Grosjean
 */
public class PDebug {
	
	public static boolean debugRegionManagement = false;	
	public static boolean debugPaintCalls = false;	
	public static boolean debugPrintFrameRate = false;	
	public static boolean debugPrintUsedMemory = false; 
	public static boolean debugBounds = false;
	public static boolean debugFullBounds = false;
	public static boolean debugThreads = false;
	public static int printResultsFrameRate = 10;
	
	private static int debugPaintColor;

	private static long framesProcessed;
	private static long startProcessingOutputTime;
	private static long startProcessingInputTime;
	private static long processOutputTime;
	private static long processInputTime;
	private static boolean processingOutput;

	private PDebug() {
		super();
	}
	
	public static Color getDebugPaintColor() {
		int color = 100 + (debugPaintColor++ % 10) * 10;
		return new Color(color, color, color, 150);
	}
	
	// called when scene graph needs update.
	public static void scheduleProcessInputs() {
		if (debugThreads && !SwingUtilities.isEventDispatchThread()) {
			System.out.println("scene graph manipulated on wrong thread");
		}
	}
	
	public static void processRepaint() {
		if (processingOutput && debugPaintCalls) {
			System.err.println("Got repaint while painting scene. This can result in a recursive process that degrades performance.");
		}
		
		if (debugThreads && !SwingUtilities.isEventDispatchThread()) {
			System.out.println("repaint called on wrong thread");
		}
	}
	
	public static boolean getProcessingOutput() {
		return processingOutput;
	}
	
	public static void startProcessingOutput() {
		processingOutput = true;
		startProcessingOutputTime = System.currentTimeMillis();
	}
	
	public static void endProcessingOutput(Graphics g) {
		processOutputTime += (System.currentTimeMillis() - startProcessingOutputTime);
		framesProcessed++;
				
		if (PDebug.debugPrintFrameRate) {
			if (framesProcessed % printResultsFrameRate == 0) {
				System.out.println("Process output frame rate: " + getOutputFPS() + " fps");
				System.out.println("Process input frame rate: " + getInputFPS() + " fps");
				System.out.println("Total frame rate: " + getTotalFPS() + " fps");
				System.out.println();				
				resetFPSTiming();				
			}
		}
		
		if (PDebug.debugPrintUsedMemory) {
			if (framesProcessed % printResultsFrameRate == 0) { 		
				System.out.println("Approximate used memory: " + getApproximateUsedMemory() / 1024 + " k");
			}
		}
		
		if (PDebug.debugRegionManagement) {
			Graphics2D g2 = (Graphics2D)g;
			g.setColor(PDebug.getDebugPaintColor());
			g2.fill(g.getClipBounds().getBounds2D());
		}
		
		processingOutput = false;
	}

	public static void startProcessingInput() {
		startProcessingInputTime = System.currentTimeMillis();
	}
	
	public static void endProcessingInput() {
		processInputTime += (System.currentTimeMillis() - startProcessingInputTime);
	}
	
	/**
	 * Return how many frames are processed and painted per second. 
	 * Note that since piccolo doesn't paint continuously this rate
	 * will be slow unless you are interacting with the system or have
	 * activities scheduled.
	 */
	public static double getTotalFPS() {
		if ((framesProcessed > 0)) {
			return 1000.0 / ((processInputTime + processOutputTime) / (double) framesProcessed);
		} else {
			return 0;
		}
	}

	/**
	 * Return the frames per second used to process 
	 * input events and activities.
	 */
	public static double getInputFPS() {
		if ((processInputTime > 0) && (framesProcessed > 0)) {
			return 1000.0 / (processInputTime / (double) framesProcessed);
		} else {
			return 0;
		}
	}
	
	/**
	 * Return the frames per seconds used to paint
	 * graphics to the screen.
	 */
	public static double getOutputFPS() {
		if ((processOutputTime > 0) && (framesProcessed > 0)) {
			return 1000.0 / (processOutputTime / (double) framesProcessed);
		} else {
			return 0;
		}
	}
	
	/**
	 * Return the number of frames that have been processed since the last
	 * time resetFPSTiming was called.
	 */
	public long getFramesProcessed() {
		return framesProcessed;
	}
	
	/**
	 * Reset the variables used to track FPS. If you reset seldom they you will
	 * get good average FPS values, if you reset more often only the frames recorded
	 * after the last reset will be taken into consideration.
	 */
	public static void resetFPSTiming() {
		framesProcessed = 0;
		processInputTime = 0;
		processOutputTime = 0;
	}
	
	public static long getApproximateUsedMemory() {
		System.gc();
		System.runFinalization();
		long totalMemory = Runtime.getRuntime().totalMemory();
		long free = Runtime.getRuntime().freeMemory();
		return totalMemory - free;
	}	
}
