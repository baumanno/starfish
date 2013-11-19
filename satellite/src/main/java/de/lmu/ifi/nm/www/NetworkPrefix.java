package de.lmu.ifi.nm.www;

import java.io.Serializable;
import java.net.InetAddress;

/**
 * This class represents the combination of network address and prefix length,
 * e.g. 10.1.0.0/24.
 * 
 * @author Dawin Schmidt, dawin.schmidt@lmu.de
 * @since October 2, 2013
 */
public class NetworkPrefix implements Serializable {
	private static final long serialVersionUID = 1L;
	private InetAddress address = null;
	private int prefixLen;

	/**
	 * @param address
	 *            - The IP-Address.
	 * @param prefixLen
	 *            - The prefix length of the corresponding IP-Address.
	 */
	public NetworkPrefix(InetAddress address, int prefixLen) {
		this.address = address;
		this.prefixLen = prefixLen;
	}

	/**
	 * @return The IP-Address.
	 */
	public InetAddress getAddress() {
		return address;
	}

	/**
	 * Set the IP-Address.
	 * 
	 * @param address
	 *            - The IP-Address to be set.
	 */
	public void setAddress(InetAddress address) {
		this.address = address;
	}

	/**
	 * @return The prefix length.
	 */
	public int getPrefixLen() {
		return prefixLen;
	}

	/**
	 * Set the prefix length.
	 * 
	 * @param prefixLen
	 *            - The prefix length to be set.
	 */
	public void setPrefixLen(int prefixLen) {
		this.prefixLen = prefixLen;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return address.toString().substring(1) + "/" + prefixLen;
	}
}