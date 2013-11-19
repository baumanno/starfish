package de.lmu.ifi.nm.www;

/**
 * @author Lance Colton
 * @author Andrew Madden
*/

public class Iandr {
	/**
	 * Takes two IP address and ANDs them
	 *
	 * @param ip1
	 *            the IP address
	 * @param ip2
	 *            the Subnet Mask
	 * @return the subnet address
	 */
	public static String and(String ip1, String ip2) {
		String[] in1 = ip1.split("[.]");
		String[] in2 = ip2.split("[.]");

		String[] out1 = new String[4];

		for (int i = 0; i < 4; i++) {
			out1[i] = Integer.toString(Integer.parseInt(in1[i])
					& Integer.parseInt(in2[i]));
		}

		String out = out1[0] + "." + out1[1] + "." + out1[2] + "." + out1[3];

		return out;
	}

	/**
	 * Takes two IP address and returns the AND'ed broadcast address
	 *
	 * @param ip1
	 *            the subnet address
	 * @param ip2
	 *            the Subnet Mask
	 * @return the broadcast address
	 */
	public static String broadcast(String ip1, String ip2) {
		char[][] ip_bin1 = Conv.ipToBin(ip1);
		char[][] ip_bin2 = Conv.ipToBin(ip2);

		char[][] ip_result = new char[4][8];

		for (int i = 0; i < 8; i++) {
			if (ip_bin2[0][i] == '0') {
				ip_result[0][i] = '1';
			} else {
				ip_result[0][i] = ip_bin1[0][i];
			}

			if (ip_bin2[1][i] == '0') {
				ip_result[1][i] = '1';
			} else {
				ip_result[1][i] = ip_bin1[1][i];
			}

			if (ip_bin2[2][i] == '0') {
				ip_result[2][i] = '1';
			} else {
				ip_result[2][i] = ip_bin1[2][i];
			}

			if (ip_bin2[3][i] == '0') {
				ip_result[3][i] = '1';
			} else {
				ip_result[3][i] = ip_bin1[3][i];
			}
		}

		return Conv.toDecimal(new String(ip_result[0])) + "."
				+ Conv.toDecimal(new String(ip_result[1])) + "."
				+ Conv.toDecimal(new String(ip_result[2])) + "."
				+ Conv.toDecimal(new String(ip_result[3]));
	}
}