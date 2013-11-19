package de.lmu.ifi.nm.www;

import java.text.DecimalFormat;

/**
 * @author Lance Colton
 * @author Andrew Madden
*/

public class Conv {
	/**
	 * convert to binary.
	 *
	 * @param dec
	 *            the decimal value
	 * @return binary string
	 */
	public static String toBinary(String dec) {
		DecimalFormat df = new DecimalFormat("00000000");
		int v = Integer.parseInt(dec);
		String s = Integer.toBinaryString(v);
		int val = Integer.parseInt(s);
		return df.format(val);
	}

	/**
	 * convert to decimal.
	 *
	 * @param bin
	 *            the binary value
	 * @return decimal string
	 */
	public static String toDecimal(String bin) {
		return Integer.toString(Integer.parseInt(bin, 2));
	}

	/**
	 * convert IP to string.
	 *
	 * @param a
	 *            the first IP field
	 * @param b
	 *            the second IP field
	 * @param c
	 *            the third IP field
	 * @param d
	 *            the fourth IP field
	 * @return the IP string
	 */
	public static String ipToString(int a, int b, int c, int d) {
		return Integer.toString(a) + "." + Integer.toString(b) + "."
				+ Integer.toString(c) + "." + Integer.toString(d);
	}

	/**
	 *
	 * @param ip
	 *            IP input
	 * @return char[][]
	 */
	public static char[][] ipToBin(String ip) {
		String[] split = ip.split("[.]");
		char[] a1 = Conv.toBinary(split[0]).toCharArray();
		char[] b1 = Conv.toBinary(split[1]).toCharArray();
		char[] c1 = Conv.toBinary(split[2]).toCharArray();
		char[] d1 = Conv.toBinary(split[3]).toCharArray();
		char[][] vec = { a1, b1, c1, d1 };
		return vec;
	}

	/**
	 * Rounds a number up to the closet multiple of 2
	 *
	 * @param toRound
	 *            number to round
	 * @since 1.2
	 */
	public static int numRound(int toRound) {
		int b = 1;

		for (int i = 0; b < toRound; i++) {
			b = (int) Math.pow(2, i);
		}

		return b;
	}
}
