package ru.nettrash.crypto;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import ru.nettrash.util.Arrays;

/**
 * Created by nettrash on 31.01.2018.
 */

public final class HMAC {

    public static int[] SHA256(int[] message, int[] key) throws Exception {
        Mac hasher = Mac.getInstance("HmacSHA256");
        hasher.init(new SecretKeySpec(Arrays.toByteArray(key), "HmacSHA256"));
        return Arrays.toUnsignedByteArray(hasher.doFinal(Arrays.toByteArray(message)));
    }

    public static byte[] SHA256(byte[] message, byte[] key) throws Exception {
        Mac hasher = Mac.getInstance("HmacSHA256");
        hasher.init(new SecretKeySpec(key, "HmacSHA256"));
        return hasher.doFinal(message);
    }
}
