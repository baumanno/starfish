package controllers.service;

import play.Logger;

/**
 * Thread that runs in the background and is iterative (after the refresh time) 
 * calculating the whole costs new and sets the routes new
 * 
 * @author Werner Hoffmann
 * @version 0.3
 *          First Implementation
 */
public class IterativeThread extends Thread {
	private static final int MULTI = 600000;
	int timeToRefresh;

	public IterativeThread(int timeToRefresh) {
		this.timeToRefresh = timeToRefresh;
	}

	/**
	 * 
	 * Main method in this thread. Monitors the time to refresh.
	 * 
	 * @return void
	 */
	public void run() {
		Logger.debug("Iterative Thread is started");
		try {
			while (!isInterrupted()) {
				sleep(MULTI*timeToRefresh);
				//TODO Knwon error: timeToRefresh is not updated, when the value in the properties file was changed
				Logger.debug("Iterative Thread is working");
				Communicator.MyLogic.dowork();
			}

		} catch (InterruptedException exception) {
		}
	}

}
