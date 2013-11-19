package models;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

import play.Logger;

/**
 * All data which are important for the runtime and should be stored in
 * database. Not worth to build each a own class. All elements are
 * standard-types and private.
 * 
 * @author Werner Hoffmann
 * @version 0.3
 */

public class ApplicationProperties {
	private static int MinutesToRefresh;
	private static String Operators;
	private static String ListeningIpFromServer;

	/**
	 * @return the minutesToRefresh
	 */
	public int getMinutesToRefresh() {
		return MinutesToRefresh;
	}

	/**
	 * @return the operators
	 */
	public String getOperators() {
		return Operators;
	}

	/**
	 * @return the operators
	 */
	public String getListeningIpFromServer() {
		return ListeningIpFromServer;
	}


	public String getFormula() {
		return Operators;
	}

	//

	public void loadProgrammProperies() {
		Logger.debug("reading config.proterties");
		FileReader fr = null;

		File inFile = new File("./conf/application.properties");
		try {
			fr = new FileReader("./conf/application.properties");

			BufferedReader br = new BufferedReader(fr);
			String line = br.readLine();
			while (line != null) {
				if (line.startsWith("#")) {
					line = br.readLine();
					continue;
				} else if (line.startsWith("MinutesToRefresh=")) {
					String[] a = line.split("\\s");
					//TODO if minutesToRefresh change the iterativeThread has to change
					MinutesToRefresh = Integer.parseInt(a[1]);
					line = br.readLine();
				} else if (line.startsWith("ListeningIpFromServer=")) {
					String[] a = line.split("\\s");
					ListeningIpFromServer = line.substring(22);
					line = br.readLine();
				} else if (line.startsWith("Operators")) {
					Operators = line.substring(10);
//					Logger.debug("Operators: "+ line);
					line = br.readLine();
				}
				line = br.readLine();
			}
			br.close();

		} catch (FileNotFoundException e) {
			Logger.error("application.properties not found in: " + inFile.getAbsolutePath(), e);
		} catch (IOException e) {
			Logger.error("IOException in application.properties" + inFile.getAbsolutePath(), e);
		}

		Logger.debug("Value of MinutesToRefresh is set to " + MinutesToRefresh + "; Value of Operators is set to " + Operators);
	}

}
