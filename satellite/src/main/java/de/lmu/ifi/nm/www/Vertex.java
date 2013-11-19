package de.lmu.ifi.nm.www;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

/**
 * This class represents a vertex in the topology graph.
 * 
 * @author Dawin Schmidt, dawin.schmidt@lmu.de
 * @since October 2, 2013
 */
public class Vertex implements Serializable {
	private static final long serialVersionUID = 1L;
	private String uniqueIdentifier;
	private String userName = null;
	private String password = null;
	private HashMap<String, List<NetworkPrefix>> interfaceHash = null;
	private String managementIP = null;
	private String routingTable = null;
	private double latitude;
	private double longitude;
	private long upTime = 0;
	private String hostname = null;

	/**
	 * @param uniqueIdentifier
	 *            - The unique identifier of the vertex.
	 * @param userName
	 *            - The user name of the vertex.
	 * @param password
	 *            - The password of the vertex.
	 * @param interfaceHash
	 *            - The interfaces of the vertex.
	 * @param routingTable
	 *            - The routing information base of the vertex.
	 * @param upTime
	 *            - The uptime of the vertex.
	 * @param hostname
	 *            - The name of the vertex.
	 * @param managementIP
	 *            - The management IP-Address of the vertex.
	 * @param latitude
	 *            - The latitude GPS coordinate of the the vertex.
	 * @param longitude
	 *            - The longitude PS coordinate of the the vertex.
	 */
	public Vertex(String uniqueIdentifier, String userName, String password,
			HashMap<String, List<NetworkPrefix>> interfaceHash,
			String routingTable, long upTime, String hostname,
			String managementIP, double latitude, double longitude) {
		super();
		this.uniqueIdentifier = uniqueIdentifier;
		this.userName = userName;
		this.password = password;
		this.interfaceHash = interfaceHash;
		this.routingTable = routingTable;
		this.upTime = upTime;
		this.hostname = hostname;
		this.managementIP = managementIP;
		this.latitude = latitude;
		this.longitude = longitude;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return getHostname();
	}

	/**
	 * @return All information about the vertex in a string representation.
	 */
	public String getLongString() {
		return "\n" + "---------- System Properties ----------" + "\n"
				+ "Hostname \t : "
				+ getHostname()
				+ "\n"
				+ "Mgmt IP \t : "
				+ getManagementIP().toString()
				+ "\n"
				+ "Identifier \t : "
				+ getUniqueIdentifier()
				+ "\n"
				+ "UserName \t : "
				+ getUserName()
				+ "\n"
				+ "Password \t : "
				+ getPassword()
				+ "\n"
				+ "UpTime \t \t : "
				+ getUpTime()
				+ "\n"
				+ "Latitude \t : "
				+ getLatitude()
				+ "\n"
				+ "Longitude \t : "
				+ getLongitude()
				+ "\n"
				+ "---------- Interfaces ----------"
				+ interfaceHashToString()
				+ "\n"
				+ "---------- Routing Table ----------"
				+ "\n"
				+ getRoutingTable();
	}

	/**
	 * @return - The unique identifier of the vertex.
	 */
	public String getUniqueIdentifier() {
		return uniqueIdentifier;
	}

	/**
	 * @return The user name of the vertex.
	 */
	public String getUserName() {
		return userName;
	}

	/**
	 * @return The management IP-Address of the vertex.
	 */
	public String getManagementIP() {
		return managementIP;
	}

	/**
	 * @return The password of the vertex.
	 */
	public String getPassword() {
		return password;
	}

	/**
	 * @return The interface HashMap of the vertex.
	 */
	public HashMap<String, List<NetworkPrefix>> getInterfaceHash() {
		return interfaceHash;
	}

	/**
	 * @return The interface HashMap of the vertex in a string representation.
	 */
	private String interfaceHashToString() {
		String interfaceString = "";
		String value = "";

		Iterator<String> hashIterator = interfaceHash.keySet().iterator();

		while (hashIterator.hasNext()) {
			String key = hashIterator.next();

			for (NetworkPrefix prefix : interfaceHash.get(key)) {
				value = value + prefix.toString();
			}

			interfaceString = interfaceString + "\n" + key + "\t : " + value;
			value = "";
		}

		return interfaceString;
	}

	/**
	 * @return The routing table of the vertex.
	 */
	public String getRoutingTable() {
		return routingTable;
	}

	/**
	 * @return The uptime of the vertex.
	 */
	public long getUpTime() {
		return upTime;
	}

	/**
	 * @return The name of the vertex.
	 */
	public String getHostname() {
		return hostname;
	}

	/**
	 * Set the unique identifier of the vertex.
	 * 
	 * @param uniqueIdentifier
	 *            - The unique identifier to be set.
	 */
	public void setUniqueIdentifier(String uniqueIdentifier) {
		this.uniqueIdentifier = uniqueIdentifier;
	}

	/**
	 * Set the user name of the vertex.
	 * 
	 * @param userName
	 *            - The user name to be set.
	 */
	public void setUserName(String userName) {
		this.userName = userName;
	}

	/**
	 * Set the password of the vertex.
	 * 
	 * @param password
	 *            - The password to be set.
	 */
	public void setPassword(String password) {
		this.password = password;
	}

	/**
	 * Set the intefaceHash HashMap.
	 * 
	 * @param interfaceHash
	 *            - The HashMap 'interfaceHash' to be set.
	 */
	public void setInterfaceHash(
			HashMap<String, List<NetworkPrefix>> interfaceHash) {
		this.interfaceHash = interfaceHash;
	}

	/**
	 * Set the RIB of the vertex.
	 * 
	 * @param routingTable
	 *            - The RIB to be set.
	 */
	public void setRouteList(String routingTable) {
		this.routingTable = routingTable;
	}

	/**
	 * Set the uptime of the vertex.
	 * 
	 * @param upTime
	 *            - The uptime to be set.
	 */
	public void setUpTime(long upTime) {
		this.upTime = upTime;
	}

	/**
	 * Set the name of the vertex.
	 * 
	 * @param hostname
	 *            - The name to be set.
	 */
	public void setHostname(String hostname) {
		this.hostname = hostname;
	}

	/**
	 * Set the management IP-Address of the vertex.
	 * 
	 * @param managementIP
	 *            - The management IP-Address to be set.
	 */
	public void setManagementIP(String managementIP) {
		this.managementIP = managementIP;
	}

	/**
	 * @return The latitude GPS coordinate of the vertex.
	 */
	public double getLatitude() {
		return latitude;
	}

	/**
	 * Set the latitude GPS coordinate of the vertex.
	 * 
	 * @param latitude
	 *            - The latitude to be set.
	 */
	public void setLatitude(double latitude) {
		this.latitude = latitude;
	}

	/**
	 * @return The longitude GPS coordinate of the vertex.
	 */
	public double getLongitude() {
		return longitude;
	}

	/**
	 * Set the longitude GPS coordinate of the vertex.
	 * 
	 * @param longitude
	 *            - The longitude to be set.
	 */
	public void setLongitude(double longitude) {
		this.longitude = longitude;
	}
}