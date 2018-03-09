package com.example.magaton.testenfcapp;

/**
 * Created by Gabriel.Magaton on 09/03/2018.
 */

public class StringUtils {

    final protected static char[] hexArray = "0123456789ABCDEF".toCharArray();

    public static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xff;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0f];
        }
        return new String(hexChars);
    }

    public static byte[] hexStringToByteArray(String s) {
        try {

            int len = s.length();
            if (len > 1) {
                byte[] data = new byte[len / 2];
                for (int i = 0; i < len; i += 2) {
                    data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                            + Character.digit(s.charAt(i + 1), 16));
                }
                return data;
            } else

            {
                return null;
            }
        } catch (Exception e) {
            throw e;
        }
    }
}
