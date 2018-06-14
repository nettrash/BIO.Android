package ru.nettrash.crypto;

/**
 * Created by nettrash on 11.01.2018.
 */

public class SHA256 extends SHA2Core {

    public SHA256()
    {
        super();
    }

    private static final int[] initVal = {
            0x6A09E667, 0xBB67AE85, 0x3C6EF372, 0xA54FF53A,
            0x510E527F, 0x9B05688C, 0x1F83D9AB, 0x5BE0CD19
    };

    int[] getInitVal()
    {
        return initVal;
    }

    public int getDigestLength()
    {
        return 32;
    }

    public Digest copy()
    {
        return copyState(new SHA256());
    }
}