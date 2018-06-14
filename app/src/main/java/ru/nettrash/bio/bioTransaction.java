package ru.nettrash.bio;

import org.jetbrains.annotations.Contract;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.UUID;

import ru.nettrash.crypto.EllipticCurve;
import ru.nettrash.crypto.HMAC;
import ru.nettrash.crypto.PointFP;
import ru.nettrash.crypto.SHA256;
import ru.nettrash.math.BigInteger;
import ru.nettrash.bio.classes.bioUnspentTransaction;
import ru.nettrash.bio.database.Address;
import ru.nettrash.util.Arrays;

/**
 * Created by nettrash on 14.06.2018.
 */

public final class bioTransaction {

    private int mVersion = 1;
    private int mLockTime = 0;
    private ArrayList<bioTransactionInput> mInput;
    private ArrayList<bioTransactionOutput> mOutput;
    private bioWallet mChange;
    private int mTimestamp;
    private String mBlock;

    public bioTransaction() {
        mInput = new ArrayList<bioTransactionInput>();
        mOutput = new ArrayList<bioTransactionOutput>();
        try {
            mChange = new bioWallet();
        } catch (Exception ex) {
            mChange = null;
        }
    }

    public void addOutput(String address, Double amount) throws Exception {
        BigInteger value = new BigInteger(String.format("%.0f", (amount * Math.pow(10, 8))));
        int[] script = bioAddress.spendToScript(address);
        mOutput.add(new bioTransactionOutput(script, value, (long)(amount * Math.pow(10, 8))));
    }

    public void addInput(bioUnspentTransaction tx) {
        mInput.add(new bioTransactionInput(tx.Address, tx.Id, Integer.valueOf(tx.N), tx.Script, Integer.valueOf(mLockTime)));
    }

    public void addChange(Double amount) throws Exception {
        mChange.initialize(UUID.randomUUID().toString());
        addOutput(mChange.Address, amount);
    }

    @Contract(pure = true)
    public bioWallet getChange() {
        return mChange;
    }

    private int[] transactionHash(bioTransactionInput input) {
        bioTransaction tx = clone();
        for (bioTransactionInput i: tx.mInput) {
            if (!i.Outpoint.Hash.equalsIgnoreCase(input.Outpoint.Hash)) {
                i.Script = new int[0];
            } else {
                i.Script = input.Script.clone();
            }
        }
        int[] data = tx.serialize();
        data = Arrays.append(data, Arrays.reverse(Arrays.toUnsignedByteArray(ByteBuffer.allocate(4).putInt(1).array())));
        SHA256 sha256 = new SHA256();
        sha256.update(Arrays.toByteArray(data));
        byte[] hash = sha256.digest();
        sha256 = new SHA256();
        sha256.update(hash);
        hash = sha256.digest();
        return Arrays.toUnsignedByteArray(hash);
    }

    private BigInteger deterministicK(ru.nettrash.bio.database.Address address, int[] hash, int badrs) throws Exception {
        int[] x = address.getPrivateKey();
        EllipticCurve curve = new EllipticCurve();
        BigInteger N = curve.N;
        int[] v = new int[32];
        java.util.Arrays.fill(v, 1);
        int[] k = new int[32];
        java.util.Arrays.fill(k, 0);
        int[] vv = v.clone();
        vv = Arrays.append(vv, 0);
        vv = Arrays.append(vv, x);
        vv = Arrays.append(vv, hash);
        k = HMAC.SHA256(vv, k);
        v = HMAC.SHA256(v, k);
        vv = v.clone();
        vv = Arrays.append(vv, 1);
        vv = Arrays.append(vv, x);
        vv = Arrays.append(vv, hash);
        k = HMAC.SHA256(vv, k);
        v = HMAC.SHA256(v, k);
        v = HMAC.SHA256(v, k);
        BigInteger KBigInt = new BigInteger(Arrays.toByteArray(v));
        int i = 0;
        BigInteger Zero = new BigInteger(0);
        while (KBigInt.compareTo(N) >= 0 || KBigInt.compareTo(Zero) <= 0 || i < badrs) {
            vv = v.clone();
            vv = Arrays.append(vv, 0);
            k = HMAC.SHA256(vv, k);
            v = HMAC.SHA256(v, k);
            v = HMAC.SHA256(v, k);
            KBigInt = new BigInteger(Arrays.toByteArray(v));
            i++;
        }
        return KBigInt;
    }

    private int[] transactionSign(bioTransactionInput input, ru.nettrash.bio.database.Address address) throws Exception {
        int[] hash = transactionHash(input);
        EllipticCurve curve = new EllipticCurve();
        int[] key = address.getPrivateKey();
        BigInteger priv = new BigInteger(Arrays.toByteArray(key));
        BigInteger n = curve.N;
        BigInteger e = new BigInteger(Arrays.toByteArray(hash));
        int badrs = 0;
        BigInteger r = new BigInteger(0);
        BigInteger s = new BigInteger(0);
        BigInteger Zero = new BigInteger(0);
        do {
            BigInteger k = deterministicK(address, hash, badrs);
            PointFP G = curve.G;
            PointFP Q = G.multiply(k);
            r = Q.getX().toBigInteger().mod(n);
            s = k.modInverse(n).multiply(e.add(priv.multiply(r))).mod(n);
            badrs++;
        } while (r.compareTo(Zero) <= 0 || s.compareTo(Zero) <= 0);
        BigInteger halfn = n.shiftRight(1);
        if (s.compareTo(halfn) > 0) {
            s = n.subtract(s);
        }
        int[] sign = serializeSign(r, s);
        return Arrays.append(sign, 1);
    }

    private int[] serializeSign(BigInteger r, BigInteger s) {
        int[] rBa = r.toByteArraySigned();
        int[] sBa = s.toByteArraySigned();

        int[] sequence = new int[0];
        sequence = Arrays.append(sequence, 0x02);
        sequence = Arrays.append(sequence, rBa.length);
        sequence = Arrays.append(sequence, rBa);
        sequence = Arrays.append(sequence, 0x02);
        sequence = Arrays.append(sequence, sBa.length);
        sequence = Arrays.append(sequence, sBa);
        sequence = Arrays.append(new int[] { sequence.length }, sequence);
        sequence = Arrays.append(new int[] { 0x30 }, sequence);
        return sequence;
    }

    private void signInput(bioTransactionInput input, Address address) throws Exception {
        //Вычисляем подпись (Script)
        int[] key = address.getPublicKey();
        int[] signature = transactionSign(input, address);
        int[] script = new int[0];
        int cnt = signature.length;
        if (cnt < 76) {
            script = Arrays.append(script, cnt);
        } else {
            if (cnt < 0xff) {
                script = Arrays.append(script, 76);
                script = Arrays.append(script, cnt);
            } else {
                if (cnt < 0xffff) {
                    script = Arrays.append(script, 77);
                    script = Arrays.append(script, cnt & 0xff);
                    script = Arrays.append(script, (cnt >> 8) & 0xff);
                } else {
                    script = Arrays.append(script, 78);
                    script = Arrays.append(script, cnt & 0xff);
                    script = Arrays.append(script, (cnt >> 8) & 0xff);
                    script = Arrays.append(script, (cnt >> 16) & 0xff);
                    script = Arrays.append(script, (cnt >> 24) & 0xff);
                }
            }
        }
        script = Arrays.append(script, signature);
        cnt = key.length;
        if (cnt < 76) {
            script = Arrays.append(script, cnt);
        } else {
            if (cnt < 0xff) {
                script = Arrays.append(script, 76);
                script = Arrays.append(script, cnt);
            } else {
                if (cnt < 0xffff) {
                    script = Arrays.append(script, 77);
                    script = Arrays.append(script, cnt & 0xff);
                    script = Arrays.append(script, (cnt >> 8) & 0xff);
                } else {
                    script = Arrays.append(script, 78);
                    script = Arrays.append(script, cnt & 0xff);
                    script = Arrays.append(script, (cnt >> 8) & 0xff);
                    script = Arrays.append(script, (cnt >> 16) & 0xff);
                    script = Arrays.append(script, (cnt >> 24) & 0xff);
                }
            }
        }
        script = Arrays.append(script, key);
        input.Script = script;
    }

    public int[] sign(Address[] addresses) throws Exception {
        for (bioTransactionInput i: mInput) {
            Address addr = null;
            for (Address a: addresses) {
                if (a.getAddress().equalsIgnoreCase(i.Outpoint.Address)) {
                    addr = a;
                    break;
                }
            }
            signInput(i, addr);
        }
        return serialize();
    }

    private int[] toVarIntBytes(long value) {
        if (value < 253) {
            return Arrays.toUnsignedByteArray(ByteBuffer.allocate(1).put((byte)value).array());
        }
        if (value < 65536) {
            int[] retVal = new int[] { 253 };
            return Arrays.append(retVal, Arrays.reverse(Arrays.toUnsignedByteArray(ByteBuffer.allocate(2).putShort((short)value).array())));
        }
        if (value < 4294967296L) {
            int[] retVal = new int[] { 254 };
            return Arrays.append(retVal, Arrays.reverse(Arrays.toUnsignedByteArray(ByteBuffer.allocate(4).putInt((int)value).array())));
        }
        int[] retVal = new int[] { 255 };
        return Arrays.append(retVal, Arrays.reverse(Arrays.toUnsignedByteArray(ByteBuffer.allocate(8).putLong(value).array())));
    }

    public int[] serialize() {
        int[] data = new int[0];
        data = Arrays.append(data, Arrays.reverse(Arrays.toUnsignedByteArray(ByteBuffer.allocate(4).putInt(mVersion).array())));

        ///////////////////
        //time
        ///////////////////

        data = Arrays.append(data, toVarIntBytes(mInput.size()));
        for (bioTransactionInput i: mInput) {
            data = Arrays.append(data, Arrays.toUnsignedByteArray(Arrays.reverse(Arrays.hexStringToByteArray(i.Outpoint.Hash))));
            data = Arrays.append(data, Arrays.reverse(Arrays.toUnsignedByteArray(ByteBuffer.allocate(4).putInt(i.Outpoint.Index).array())));
            data = Arrays.append(data, toVarIntBytes(i.Script.length));
            if (i.Script.length > 0) {
                data = Arrays.append(data, i.Script);
            }
            data = Arrays.append(data, Arrays.reverse(Arrays.toUnsignedByteArray(ByteBuffer.allocate(4).putInt(i.Sequence.intValue()).array())));
        }
        data = Arrays.append(data, toVarIntBytes(mOutput.size()));
        for (bioTransactionOutput o: mOutput) {
            data = Arrays.append(data, Arrays.reverse(Arrays.toUnsignedByteArray(ByteBuffer.allocate(8).putLong(o.Satoshi).array())));
            data = Arrays.append(data, toVarIntBytes(o.ScriptedAddress.length));
            if (o.ScriptedAddress.length > 0) {
                data = Arrays.append(data, o.ScriptedAddress);
            }
        }
        data = Arrays.append(data, Arrays.reverse(Arrays.toUnsignedByteArray(ByteBuffer.allocate(4).putInt(mLockTime).array())));
        return data;
    }

    public bioTransaction clone() {
        bioTransaction retVal = new bioTransaction();
        retVal.mVersion = this.mVersion;
        retVal.mLockTime = this.mLockTime;
        retVal.mInput = new ArrayList<bioTransactionInput>();
        for (bioTransactionInput i: this.mInput) {
            retVal.mInput.add(new bioTransactionInput(i.Outpoint.Address, i.Outpoint.Hash, i.Outpoint.Index, i.Script, this.mLockTime));
        }
        if (this.mOutput != null) {
            retVal.mOutput = new ArrayList<bioTransactionOutput>();
            for (bioTransactionOutput o : this.mOutput) {
                retVal.mOutput.add(new bioTransactionOutput(o.ScriptedAddress, o.Amount, o.Satoshi));
            }
        }
        retVal.mTimestamp = this.mTimestamp;
        retVal.mBlock = this.mBlock;
        retVal.mChange.Address = this.mChange.Address;
        if (this.mChange.PrivateKey != null)
            retVal.mChange.PrivateKey = this.mChange.PrivateKey.clone();
        if (this.mChange.PublicKey != null)
            retVal.mChange.PublicKey = this.mChange.PublicKey.clone();
        retVal.mChange.WIF = this.mChange.WIF;
        return retVal;
    }
}
