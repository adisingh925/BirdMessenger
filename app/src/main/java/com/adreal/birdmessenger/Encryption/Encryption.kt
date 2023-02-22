package com.adreal.birdmessenger.Encryption

import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.security.keystore.KeyProtection
import android.util.Base64
import androidx.annotation.RequiresApi
import com.adreal.birdmessenger.Model.EncryptedData
import com.adreal.birdmessenger.SharedPreferences.SharedPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.bouncycastle.crypto.digests.SHA256Digest
import org.bouncycastle.crypto.generators.HKDFBytesGenerator
import org.bouncycastle.crypto.params.HKDFParameters
import org.bouncycastle.jce.ECNamedCurveTable
import org.bouncycastle.jce.interfaces.ECPrivateKey
import org.bouncycastle.jce.interfaces.ECPublicKey
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.jce.spec.ECNamedCurveParameterSpec
import org.bouncycastle.jce.spec.ECParameterSpec
import org.bouncycastle.jce.spec.ECPrivateKeySpec
import org.bouncycastle.jce.spec.ECPublicKeySpec
import java.math.BigInteger
import java.security.*
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import javax.crypto.Cipher
import javax.crypto.KeyAgreement
import javax.crypto.Mac
import javax.crypto.SecretKey
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

@RequiresApi(Build.VERSION_CODES.P)
class Encryption {
    companion object {
        const val DH_ALGORITHM = "DH"
        const val PROVIDER = "AndroidKeyStore"
        const val KEY_ALIAS = "messaging_key"
        private const val HMAC_ALGORITHM = "HmacSHA256"
        private const val AES_ALGORITHM = KeyProperties.KEY_ALGORITHM_AES
        private const val RSA_ALGORITHM = KeyProperties.KEY_ALGORITHM_RSA
        private const val BLOCK_MODE_RSA = KeyProperties.BLOCK_MODE_ECB
        private const val BLOCK_MODE_AES = KeyProperties.BLOCK_MODE_CBC
        private const val PADDING_RSA = KeyProperties.ENCRYPTION_PADDING_RSA_PKCS1
        private const val PADDING_AES = KeyProperties.ENCRYPTION_PADDING_PKCS7
        private const val TRANSFORMATION_RSA = "$RSA_ALGORITHM/$BLOCK_MODE_RSA/$PADDING_RSA"
        private const val TRANSFORMATION_AES = "$AES_ALGORITHM/$BLOCK_MODE_AES/$PADDING_AES"
        private const val AES_SYMMETRIC_KEY = "AESSymmetricKey"
        private const val DH_KEY_SIZE = 2048
        const val DH_PRIVATE = "DHPrivate"
        const val EC_PRIVATE = "ECDHPrivate"
        const val ELLIPTIC_CURVE_ALGORITHM = "ECDH"
        const val CURVE_NAME = "secp256r1"
    }

    fun addBouncyCastleProvider() {
        Security.removeProvider(BouncyCastleProvider.PROVIDER_NAME)
        Security.addProvider(BouncyCastleProvider())
    }

    fun generateDHKeyPair(): KeyPair {
        val keyPairGenerator = KeyPairGenerator.getInstance(DH_ALGORITHM)
        keyPairGenerator.initialize(DH_KEY_SIZE, SecureRandom())
        return keyPairGenerator.generateKeyPair()
    }

    fun getAsymmetricKeyPair(): KeyPair {
        val keyStore = KeyStore.getInstance(PROVIDER)
        keyStore.load(null)

        val privateKey = keyStore.getKey(KEY_ALIAS, null) as PrivateKey?
        val publicKey = keyStore.getCertificate(KEY_ALIAS)?.publicKey

        return if (privateKey != null && publicKey != null) {
            KeyPair(publicKey, privateKey)
        } else {
            createAsymmetricKeyPair()
        }
    }

    private fun createAsymmetricKeyPair(): KeyPair {
        val generator: KeyPairGenerator =
            KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_RSA, PROVIDER)
        val builder = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_ECB)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_RSA_PKCS1)
            .setKeySize(4096)

        generator.initialize(builder.build())

        return generator.generateKeyPair()
    }

    fun generateECDHKeyPair(): KeyPair {
        val ecSpec = ECNamedCurveTable.getParameterSpec(CURVE_NAME)
        val keyGen = KeyPairGenerator.getInstance(ELLIPTIC_CURVE_ALGORITHM, BouncyCastleProvider())
        keyGen.initialize(ecSpec, SecureRandom())
        return keyGen.generateKeyPair()
    }

    fun generateECDHSecret(publicSecret: String, senderId: String) {
        val publicKey = getECDHPublicKeyFromBase64String(publicSecret)
        val privateKey = getECDHPrivateKeyFromBase64String()
        val sharedSecret = getECDHSharedSecret(publicKey, privateKey)
        val aesKey = getAESKeyFromSharedSecret(sharedSecret)
        SharedPreferences.write(
            "$AES_SYMMETRIC_KEY--$senderId",
            java.util.Base64.getEncoder().encodeToString(aesKey.encoded)
        )
    }

    private fun getECDHSharedSecret(publicKey: ECPublicKey, privateKey: ECPrivateKey): ByteArray {
        val keyAgreement =
            KeyAgreement.getInstance(ELLIPTIC_CURVE_ALGORITHM, BouncyCastleProvider.PROVIDER_NAME)
        keyAgreement.init(privateKey, SecureRandom())
        val keyFactory = KeyFactory.getInstance("EC", BouncyCastleProvider.PROVIDER_NAME)
        val ecPublicKey =
            keyFactory.generatePublic(X509EncodedKeySpec(publicKey.encoded)) as ECPublicKey
        keyAgreement.doPhase(ecPublicKey, true)
        return keyAgreement.generateSecret()
    }

    private fun getECDHPrivateKeyFromBase64String(): ECPrivateKey {
        val privateSecret = SharedPreferences.read(EC_PRIVATE, "")
        val priKey = java.util.Base64.getDecoder().decode(privateSecret)
        val keySpec = PKCS8EncodedKeySpec(priKey)
        val keyFactory = KeyFactory.getInstance("EC", BouncyCastleProvider.PROVIDER_NAME)
        return keyFactory.generatePrivate(keySpec) as ECPrivateKey
    }

    private fun getECDHPublicKeyFromBase64String(publicKeyBase64: String): ECPublicKey {
        val publicKeyBytes = java.util.Base64.getDecoder().decode(publicKeyBase64)
        val keySpec = X509EncodedKeySpec(publicKeyBytes)
        val keyFactory = KeyFactory.getInstance("EC", BouncyCastleProvider.PROVIDER_NAME)
        return keyFactory.generatePublic(keySpec) as ECPublicKey
    }

    fun generateDHSecret(publicSecret: String, senderId: String) {
        CoroutineScope(Dispatchers.IO).launch {
            val publicKey = getDHPublicKeyFromBase64String(publicSecret)
            val privateKey = getDHPrivateKeyFromBase64String()
            val sharedSecret = getDHSharedSecret(publicKey, privateKey)
            val aesKey = getAESKeyFromSharedSecret(sharedSecret)
            SharedPreferences.write(
                "$AES_SYMMETRIC_KEY--$senderId",
                java.util.Base64.getEncoder().encodeToString(aesKey.encoded)
            )
        }
    }

    private fun getAESKeyFromSharedSecret(sharedSecret: ByteArray): SecretKeySpec {
        val hashBasedKeyDerivation = HKDFBytesGenerator(SHA256Digest())
        val derivedKey = ByteArray(32)
        hashBasedKeyDerivation.init(HKDFParameters(sharedSecret, null, null))
        hashBasedKeyDerivation.generateBytes(derivedKey, 0, 32)
        return SecretKeySpec(derivedKey, AES_ALGORITHM)
    }

    private fun getDHSharedSecret(publicKey: PublicKey, privateKey: PrivateKey): ByteArray {
        val clientKeyAgreement: KeyAgreement = KeyAgreement.getInstance(DH_ALGORITHM)
        clientKeyAgreement.init(privateKey)
        clientKeyAgreement.doPhase(publicKey, true)
        return clientKeyAgreement.generateSecret()
    }

    private fun getDHPrivateKeyFromBase64String(): PrivateKey {
        val privateSecret = SharedPreferences.read(DH_PRIVATE, "")
        val priKey = java.util.Base64.getDecoder().decode(privateSecret)
        val keySpecPrivate = PKCS8EncodedKeySpec(priKey)
        val keyFactoryPrivate = KeyFactory.getInstance(DH_ALGORITHM)
        return keyFactoryPrivate.generatePrivate(keySpecPrivate)
    }

    private fun getDHPublicKeyFromBase64String(publicKeyBase64: String): PublicKey {
        val publicKeyString = java.util.Base64.getDecoder().decode(publicKeyBase64)
        val publicKeySpecification = X509EncodedKeySpec(publicKeyString)
        val keyFactory = KeyFactory.getInstance(DH_ALGORITHM)
        return keyFactory.generatePublic(publicKeySpecification)
    }

    fun encrypt(data: String, publicKey: String): String {
        val key = java.util.Base64.getDecoder().decode(publicKey)

        val keySpec = X509EncodedKeySpec(key)
        val keyFactory = KeyFactory.getInstance(RSA_ALGORITHM)
        val pubKey = keyFactory.generatePublic(keySpec)

        val cipher: Cipher = Cipher.getInstance(TRANSFORMATION_RSA)
        cipher.init(Cipher.ENCRYPT_MODE, pubKey)
        val bytes = cipher.doFinal(data.toByteArray())
        return Base64.encodeToString(bytes, Base64.DEFAULT)
    }

    fun decrypt(data: String): String {
        val cipher: Cipher = Cipher.getInstance(TRANSFORMATION_RSA)
        cipher.init(Cipher.DECRYPT_MODE, getAsymmetricKeyPair().private)
        val encryptedData = Base64.decode(data, Base64.DEFAULT)
        val decodedData = cipher.doFinal(encryptedData)
        return String(decodedData)
    }

    fun encryptUsingSymmetricKey(data: String, id: String): EncryptedData {
        val cipher = Cipher.getInstance(TRANSFORMATION_AES)
        cipher.init(Cipher.ENCRYPT_MODE, getStoredSymmetricEncryptionKey(id), SecureRandom())
        val iv = cipher.iv
        val plaintext = data.toByteArray()
        val ciphertext = cipher.doFinal(plaintext)
        return EncryptedData(ciphertext, iv)
    }

    fun decryptUsingSymmetricEncryption(
        cipherText: ByteArray,
        iv: ByteArray,
        id: String
    ): String {
        val cipher = Cipher.getInstance(TRANSFORMATION_AES)
        val ivParameterSpec = IvParameterSpec(iv)
        cipher.init(Cipher.DECRYPT_MODE, getStoredSymmetricEncryptionKey(id), ivParameterSpec)
        return cipher.doFinal(cipherText).decodeToString()
    }


    fun generateHMAC(message: String, id: String): String {
        try {
            val mac = Mac.getInstance(HMAC_ALGORITHM)
            mac.init(getStoredSymmetricEncryptionKey(id))
            val hash = mac.doFinal(message.toByteArray())
            return hash.joinToString("") { String.format("%02x", it) }
        } catch (e: NoSuchAlgorithmException) {
            e.printStackTrace()
        } catch (e: InvalidKeyException) {
            e.printStackTrace()
        }
        return ""
    }

    fun compareMessageAndHMAC(msg: String, hash: String, id: String): Boolean {
        return generateHMAC(msg, id) == hash
    }

    @RequiresApi(Build.VERSION_CODES.P)
    fun getStoredSymmetricEncryptionKey(id: String): SecretKeySpec {
        return SecretKeySpec(
            java.util.Base64.getDecoder()
                .decode(SharedPreferences.read("$AES_SYMMETRIC_KEY--$id", "")), AES_ALGORITHM
        )
    }
}