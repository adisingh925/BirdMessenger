package com.adreal.birdmessenger.Encryption

import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import android.util.Log
import androidx.annotation.RequiresApi
import com.adreal.birdmessenger.Model.EncryptedData
import com.adreal.birdmessenger.SharedPreferences.SharedPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.bouncycastle.crypto.digests.SHA256Digest
import org.bouncycastle.crypto.generators.HKDFBytesGenerator
import org.bouncycastle.crypto.params.HKDFParameters
import java.security.*
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import javax.crypto.Cipher
import javax.crypto.KeyAgreement
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

class Encryption {
    companion object {
        const val DH_ALGORITHM = "DH"
        const val PROVIDER = "AndroidKeyStore"
        const val KEY_ALIAS = "messaging_key"
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
        val builder = KeyGenParameterSpec.Builder(KEY_ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
            .setBlockModes(KeyProperties.BLOCK_MODE_ECB)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_RSA_PKCS1)
            .setKeySize(4096)

        generator.initialize(builder.build())

        return generator.generateKeyPair()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun generateSecret(publicSecret: String) {
        CoroutineScope(Dispatchers.IO).launch {
            val publicKey = getDHPublicKeyFromBase64String(publicSecret)
            val privateKey = getDHPrivateKeyFromBase64String()
            val sharedSecret = getDHSharedSecret(publicKey, privateKey)
            val aesKey = getAESKeyFromSharedSecret(sharedSecret)
            SharedPreferences.write(AES_SYMMETRIC_KEY,java.util.Base64.getEncoder().encodeToString(aesKey.encoded))
        }
    }

    private fun getAESKeyFromSharedSecret(sharedSecret: ByteArray): SecretKeySpec {
        val hashBasedKeyDerivation = HKDFBytesGenerator(SHA256Digest())
        val derivedKey = ByteArray(32)
        hashBasedKeyDerivation.init(HKDFParameters(sharedSecret,null,null))
        hashBasedKeyDerivation.generateBytes(derivedKey, 0, 32)
        return SecretKeySpec(derivedKey, AES_ALGORITHM)
    }

    private fun getDHSharedSecret(publicKey: PublicKey, privateKey: PrivateKey): ByteArray {
        val clientKeyAgreement: KeyAgreement = KeyAgreement.getInstance(DH_ALGORITHM)
        clientKeyAgreement.init(privateKey)
        clientKeyAgreement.doPhase(publicKey, true)
        return clientKeyAgreement.generateSecret()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun getDHPrivateKeyFromBase64String() : PrivateKey{
        val privateSecret = SharedPreferences.read(DH_PRIVATE, "")
        val priKey = java.util.Base64.getDecoder().decode(privateSecret)
        val keySpecPrivate = PKCS8EncodedKeySpec(priKey)
        val keyFactoryPrivate = KeyFactory.getInstance(DH_ALGORITHM)
        return keyFactoryPrivate.generatePrivate(keySpecPrivate)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun getDHPublicKeyFromBase64String(publicKeyBase64 : String) : PublicKey{
        val publicKeyString = java.util.Base64.getDecoder().decode(publicKeyBase64)
        val publicKeySpecification = X509EncodedKeySpec(publicKeyString)
        val keyFactory = KeyFactory.getInstance(DH_ALGORITHM)
        return keyFactory.generatePublic(publicKeySpecification)
    }

    @RequiresApi(Build.VERSION_CODES.O)
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

    @RequiresApi(Build.VERSION_CODES.O)
    fun encryptUsingSymmetricKey(data : String) : EncryptedData{
        val secretKey = java.util.Base64.getDecoder().decode(SharedPreferences.read(AES_SYMMETRIC_KEY,""))
        val secretKeySpec = SecretKeySpec(secretKey, AES_ALGORITHM)
        val cipher = Cipher.getInstance(TRANSFORMATION_AES)
        cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec)
        val iv = cipher.iv
        val plaintext = data.toByteArray()
        val ciphertext = cipher.doFinal(plaintext)
        return EncryptedData(ciphertext,iv)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun decryptUsingSymmetricEncryption(cipherText : ByteArray, iv : ByteArray) : String{
        val secretKey = java.util.Base64.getDecoder().decode(SharedPreferences.read(AES_SYMMETRIC_KEY,""))
        val secretKeySpec = SecretKeySpec(secretKey, AES_ALGORITHM)
        val cipher = Cipher.getInstance(TRANSFORMATION_AES)
        val ivParameterSpec = IvParameterSpec(iv)
        cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, ivParameterSpec)
        val plaintext = cipher.doFinal(cipherText)
        return plaintext.decodeToString()
    }
}