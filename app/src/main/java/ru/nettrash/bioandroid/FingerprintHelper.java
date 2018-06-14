package ru.nettrash.bioandroid;

/**
 * Created by nettrash on 09.02.2018.
 */
import android.app.KeyguardManager;
import android.content.Context;
import android.hardware.fingerprint.FingerprintManager;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyPermanentlyInvalidatedException;
import android.security.keystore.KeyProperties;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.hardware.fingerprint.FingerprintManagerCompat;
import android.util.Base64;

import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.MGF1ParameterSpec;
import java.security.spec.X509EncodedKeySpec;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.OAEPParameterSpec;
import javax.crypto.spec.PSource;

import static android.content.Context.KEYGUARD_SERVICE;

public class FingerprintHelper extends FingerprintManagerCompat.AuthenticationCallback {

    @Override
    public void onAuthenticationHelp(int helpCode, CharSequence helpString) {
        //грязные пальчики, недостаточно сильный зажим
        //можно показать helpString в виде тоста
    }

    @Override
    public void onAuthenticationFailed() {
        //отпечаток считался, но не распознался
    }

    @Override
    public void onAuthenticationError(int errorCode, CharSequence errString) {
        //несколько неудачных попыток считывания (5)
        //после этого сенсор станет недоступным на некоторое время (30 сек)
    }

    @Override
    public void onAuthenticationSucceeded(@NonNull FingerprintManagerCompat.AuthenticationResult result) {
        //все прошло успешно
    }

    private static final String keyStoreName = "BIOWalletKeyStore";
    private static final String keyAlias = "BIOWalletPINStoreKey";

    public static boolean checkFingerprintCompatibility(@NonNull Context context) {
        return FingerprintManagerCompat.from(context).isHardwareDetected();
    }

    public static SensorState checkSensorState(@NonNull Context context) {
        if (checkFingerprintCompatibility(context)) {
            KeyguardManager keyguardManager =
                    (KeyguardManager) context.getSystemService(KEYGUARD_SERVICE);
            if (!keyguardManager.isKeyguardSecure()) {
                return SensorState.NOT_BLOCKED;
            }
            FingerprintManagerCompat fingerprintManager = FingerprintManagerCompat.from(context);
            if (!fingerprintManager.hasEnrolledFingerprints()) {
                return SensorState.NO_FINGERPRINTS;
            }
            return SensorState.READY;
        } else {
            return SensorState.NOT_SUPPORTED;
        }
    }

    private static KeyStore sKeyStore;

    private static boolean getKeyStore() {
        try {
            sKeyStore = KeyStore.getInstance(keyStoreName);
            sKeyStore.load(null);
            return true;
        } catch (KeyStoreException | IOException | NoSuchAlgorithmException | CertificateException e) {
            e.printStackTrace();
        }
        return false;
    }

    private static KeyPairGenerator sKeyPairGenerator;

    private static boolean getKeyPairGenerator() {
        try {
            sKeyPairGenerator = KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_RSA, keyStoreName);
            return true;
        } catch (NoSuchAlgorithmException | NoSuchProviderException e) {
            e.printStackTrace();
        }
        return false;
    }

    private static boolean generateNewKey() {
        if (getKeyPairGenerator()) {
            try {
                sKeyPairGenerator.initialize(new KeyGenParameterSpec.Builder(keyAlias,
                        KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                        .setDigests(KeyProperties.DIGEST_SHA256, KeyProperties.DIGEST_SHA512)
                        .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_RSA_OAEP)
                        .setUserAuthenticationRequired(true)
                        .build());
                sKeyPairGenerator.generateKeyPair();
                return true;
            } catch (InvalidAlgorithmParameterException e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    private static boolean isKeyReady() {
        try {
            return sKeyStore.containsAlias(keyAlias) || generateNewKey();
        } catch (KeyStoreException e) {
            e.printStackTrace();
        }
        return false;
    }

    private static Cipher sCipher;

    private static boolean getCipher() {
        try {
            sCipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding");
            return true;
        } catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
            e.printStackTrace();
        }
        return false;
    }

    private static void initDecodeCipher(int mode) throws KeyStoreException, NoSuchAlgorithmException, UnrecoverableKeyException, InvalidKeyException {
        PrivateKey key = (PrivateKey) sKeyStore.getKey(keyAlias, null);
        sCipher.init(mode, key);
    }

    private static void initEncodeCipher(int mode) throws KeyStoreException, InvalidKeySpecException, NoSuchAlgorithmException, InvalidKeyException, InvalidAlgorithmParameterException {
        PublicKey key = sKeyStore.getCertificate(keyAlias).getPublicKey();
        PublicKey unrestricted = KeyFactory.getInstance(key.getAlgorithm()).generatePublic(new X509EncodedKeySpec(key.getEncoded()));
        OAEPParameterSpec spec = new OAEPParameterSpec("SHA-256", "MGF1", MGF1ParameterSpec.SHA1, PSource.PSpecified.DEFAULT);
        sCipher.init(mode, unrestricted, spec);
    }

    public static void deleteInvalidKey() {
        if (getKeyStore()) {
            try {
                sKeyStore.deleteEntry(keyAlias);
            } catch (KeyStoreException e) {
                e.printStackTrace();
            }
        }
    }

    private static boolean initCipher(int mode) {
        try {
            sKeyStore.load(null);
            switch (mode) {
                case Cipher.ENCRYPT_MODE:
                    initEncodeCipher(mode);
                    break;
                case Cipher.DECRYPT_MODE:
                    initDecodeCipher(mode);
                    break;
                default:
                    return false; //this cipher is only for encode\decode
            }
            return true;
        } catch (KeyPermanentlyInvalidatedException exception) {
            deleteInvalidKey();
        } catch (KeyStoreException | CertificateException | UnrecoverableKeyException | IOException |
                NoSuchAlgorithmException | InvalidKeyException | InvalidKeySpecException | InvalidAlgorithmParameterException e) {
            e.printStackTrace();
        }
        return false;
    }

    private static boolean prepare() {
        return getKeyStore() && getCipher() && isKeyReady();
    }

    @Nullable
    public static String encode(String inputString) {
        try {
            if (prepare() && initCipher(Cipher.ENCRYPT_MODE)) {
                byte[] bytes = sCipher.doFinal(inputString.getBytes());
                return Base64.encodeToString(bytes, Base64.NO_WRAP);
            }
        } catch (IllegalBlockSizeException | BadPaddingException exception) {
            exception.printStackTrace();
        }
        return null;
    }

    @Nullable
    public static String decode(String encodedString, Cipher cipherDecrypter) {
        try {
            byte[] bytes = Base64.decode(encodedString, Base64.NO_WRAP);
            return new String(cipherDecrypter.doFinal(bytes));
        } catch (IllegalBlockSizeException | BadPaddingException exception) {
            exception.printStackTrace();
        }
        return null;
    }

    @Nullable
    public static FingerprintManagerCompat.CryptoObject getCryptoObject() {
        if (prepare() && initCipher(Cipher.DECRYPT_MODE)) {
            return new FingerprintManagerCompat.CryptoObject(sCipher);
        }
        return null;
    }

    public enum SensorState {
        NOT_SUPPORTED,
        NOT_BLOCKED, // если устройство не защищено пином, рисунком или паролем
        NO_FINGERPRINTS, // если на устройстве нет отпечатков
        READY
    }
}
