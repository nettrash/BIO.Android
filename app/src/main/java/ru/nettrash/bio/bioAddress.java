package ru.nettrash.bio;

import java.util.Arrays;

import ru.nettrash.crypto.RIPEMD160;
import ru.nettrash.crypto.SHA256;
import ru.nettrash.math.BigInteger;

/*
 * Created by nettrash on 14.06.2018.
 */

public final class bioAddress {

    private static String Alphabet = "123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz";
    private static int Size = 25;

    private static int[] decodeBase58(String input) throws Exception {
        int[] output = new int[Size];
        Arrays.fill(output, 0);

        for (int i=0; i<input.length(); i++) {
            char t = input.charAt(i);
            int p = Alphabet.indexOf(t);

            if (p < 0) {
                throw new ArrayIndexOutOfBoundsException();
            }

            int j = Size;

            while (j > 0) {
                j = j - 1;
                p = p + 58 * output[j];

                output[j] = p % 256;

                p = p / 256;
            }

            if (p != 0) {
                throw new IndexOutOfBoundsException();
            }
        }

        return output;
    }

    private static int[] decodeBase58Key(String input) throws Exception {
        BigInteger base = new BigInteger(58);
        BigInteger bi = new BigInteger(0);
        int leadingZerosNum = 0;

        for (int i=input.length()-1; i>=0; i--) {

            int alphaIndex = Alphabet.indexOf(input.charAt(i));
            if (alphaIndex < 0) throw new IndexOutOfBoundsException();

            bi = bi.add(new BigInteger(alphaIndex).multiply(base.power(input.length() - 1 - i)));

            if (input.charAt(i) == '1') {
                leadingZerosNum += 1;
            } else {
                leadingZerosNum = 0;
            }
        }

        int[] bytes = bi.toByteArrayUnsigned();
        int[] result = new int[bytes.length+leadingZerosNum];
        Arrays.fill(result, 0);
        System.arraycopy(bytes, 0, result, leadingZerosNum, bytes.length);
        return result;
    }

    private static String encodeBase58(int[] data) throws Exception {
        BigInteger base = new BigInteger(58);
        BigInteger bi = new BigInteger(ru.nettrash.util.Arrays.toByteArray(data));
        String chars = "";

        while (bi.compareTo(base) >= 0) {
            BigInteger module = bi.mod(base);
            chars = Alphabet.charAt(module.intValue()) + chars;
            bi = bi.subtract(module).divide(base);
        }
        chars = Alphabet.charAt(bi.intValue()) + chars;
        for (int i=0; i<data.length; i++) {
            if (data[i] == 0x00) {
                chars = Alphabet.substring(0, 1) + chars;
            } else {
                break;
            }
        }
        return chars;
    }

    public static boolean verifySIB(String address) {
        try {
            if (address == null) return false;
            if (address.length() < 26 || address.length() > 35) return false;
            if (address.charAt(0) != 'S') return false;

            int[] decoded = decodeBase58(address);
            int[] dd = ru.nettrash.util.Arrays.subarray(decoded, 0, 21);
            SHA256 sha256 = new SHA256();
            sha256.update(ru.nettrash.util.Arrays.toByteArray(dd));
            byte[] d1 = sha256.digest();
            sha256 = new SHA256();
            sha256.update(d1);
            byte[] d2 = sha256.digest();
            int[] id2 = ru.nettrash.util.Arrays.toUnsignedByteArray(d2);
            if (decoded[21] != id2[0] ||
                    decoded[22] != id2[1] ||
                    decoded[23] != id2[2] ||
                    decoded[24] != id2[3]) {
                return false;
            }

            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    public static boolean verifyBTC(String address) {
        try {
            if (address == null) return false;
            if (address.length() < 26 || address.length() > 35) return false;
            if (address.charAt(0) != '1' && address.charAt(0) != '3') return false;

            int[] decoded = decodeBase58(address);
            int[] dd = ru.nettrash.util.Arrays.subarray(decoded, 0, 21);
            SHA256 sha256 = new SHA256();
            sha256.update(ru.nettrash.util.Arrays.toByteArray(dd));
            byte[] d1 = sha256.digest();
            sha256 = new SHA256();
            sha256.update(d1);
            byte[] d2 = sha256.digest();
            int[] id2 = ru.nettrash.util.Arrays.toUnsignedByteArray(d2);
            if (decoded[21] != id2[0] ||
                    decoded[22] != id2[1] ||
                    decoded[23] != id2[2] ||
                    decoded[24] != id2[3]) {
                return false;
            }

            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    public static boolean verify(String address) {
        try {
            if (address == null) return false;
            if (address.length() < 26 || address.length() > 35) return false;
            if (address.charAt(0) != 'B') return false;

            int[] decoded = decodeBase58(address);
            int[] dd = ru.nettrash.util.Arrays.subarray(decoded, 0, 21);
            SHA256 sha256 = new SHA256();
            sha256.update(ru.nettrash.util.Arrays.toByteArray(dd));
            byte[] d1 = sha256.digest();
            sha256 = new SHA256();
            sha256.update(d1);
            byte[] d2 = sha256.digest();
            int[] id2 = ru.nettrash.util.Arrays.toUnsignedByteArray(d2);
            if (decoded[21] != id2[0] ||
                    decoded[22] != id2[1] ||
                    decoded[23] != id2[2] ||
                    decoded[24] != id2[3]) {
                return false;
            }

            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    public static String forKey(int[] key) throws Exception {
        SHA256 sha256 = new SHA256();
        sha256.update(ru.nettrash.util.Arrays.toByteArray(key));
        byte[] keyHash = sha256.digest();
        RIPEMD160 md = new RIPEMD160();
        md.update(keyHash);
        byte[] hashData = md.digest();
        byte[] hashDataF = new byte[hashData.length+1];
        System.arraycopy(hashData, 0, hashDataF, 1, hashData.length);
        hashDataF[0] = 0x19;
        sha256 = new SHA256();
        sha256.update(hashDataF);
        keyHash = sha256.digest();
        sha256 = new SHA256();
        sha256.update(keyHash);
        byte[] hash = sha256.digest();
        hashDataF = Arrays.copyOf(hashDataF, hashDataF.length+4);
        System.arraycopy(hash, 0, hashDataF, hashDataF.length-4, 4);
        return encodeBase58(ru.nettrash.util.Arrays.toUnsignedByteArray(hashDataF));
    }

    public static String wifFromPrivateKey(int[] key) throws Exception {
        return wifFromPrivateKey(key, true);
    }

    public static String wifFromPrivateKey(int[] key, boolean compressed) throws Exception {
        byte[] d = ru.nettrash.util.Arrays.toByteArray(key);
        if (compressed) {
            d = Arrays.copyOf(d, d.length+1);
            d[d.length-1] = 0x01;
        }
        int[] retVal = new int[d.length+1];
        System.arraycopy(ru.nettrash.util.Arrays.toUnsignedByteArray(d), 0, retVal, 1, d.length);
        retVal[0] = 0x80;

        SHA256 sha256 = new SHA256();
        sha256.update(ru.nettrash.util.Arrays.toByteArray(retVal));
        d = sha256.digest();
        sha256 = new SHA256();
        sha256.update(d);
        int[] hash = ru.nettrash.util.Arrays.toUnsignedByteArray(sha256.digest());
        retVal = Arrays.copyOf(retVal, retVal.length+4);
        System.arraycopy(hash, 0, retVal, retVal.length-4, 4);
        return encodeBase58(retVal);
    }

    static int[] spendToScript(String address) throws Exception {
        int[] addrBytes = decodeBase58(address);
        int[] retVal = new int[0];
        retVal = ru.nettrash.util.Arrays.append(retVal, 118); //OP_DUP
        retVal = ru.nettrash.util.Arrays.append(retVal, 169); //HASH_160
        int cnt = addrBytes.length - 5;
        if (cnt < 76) {
            retVal = ru.nettrash.util.Arrays.append(retVal, cnt);
        } else {
            if (cnt < 0xff) {
                retVal = ru.nettrash.util.Arrays.append(retVal, 76);
                retVal = ru.nettrash.util.Arrays.append(retVal, cnt);
            } else {
                if (cnt < 0xffff) {
                    retVal = ru.nettrash.util.Arrays.append(retVal, 77);
                    retVal = ru.nettrash.util.Arrays.append(retVal, cnt & 0xff);
                    retVal = ru.nettrash.util.Arrays.append(retVal, (cnt >> 8) & 0xff);
                } else {
                    retVal = ru.nettrash.util.Arrays.append(retVal, 78);
                    retVal = ru.nettrash.util.Arrays.append(retVal, cnt & 0xff);
                    retVal = ru.nettrash.util.Arrays.append(retVal, (cnt >> 8) & 0xff);
                    retVal = ru.nettrash.util.Arrays.append(retVal, (cnt >> 16) & 0xff);
                    retVal = ru.nettrash.util.Arrays.append(retVal, (cnt >> 24) & 0xff);
                }
            }
        }
        retVal = ru.nettrash.util.Arrays.append(retVal, addrBytes, 1, addrBytes.length-5);
        retVal = ru.nettrash.util.Arrays.append(retVal, 136); //OP_EQUALVERIFY
        retVal = ru.nettrash.util.Arrays.append(retVal, 172); //OP_CHECKSIG

        return retVal;
    }

}
