package ru.nettrash.crypto;

/**
 * Created by nettrash on 11.01.2018.
 */

public interface Digest {

    public void update(byte in);

    public void update(byte[] inbuf);

    public void update(byte[] inbuf, int off, int len);

    public byte[] digest();

    public byte[] digest(byte[] inbuf);

    public int digest(byte[] outbuf, int off, int len);

    public int getDigestLength();

    public void reset();

    public Digest copy();

    public int getBlockLength();

    public String toString();

}