package com.adreal.birdmessenger.Encryption

import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import android.util.Log
import androidx.annotation.RequiresApi
import com.adreal.birdmessenger.SharedPreferences.SharedPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.security.*
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import javax.crypto.Cipher
import javax.crypto.KeyAgreement
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec

class Encryption {
    companion object {
        const val DH_ALGORITHM = "DH"
        const val PROVIDER = "AndroidKeyStore"
        const val KEY_ALIAS = "BMKEY"
        private const val RSA_ALGORITHM = KeyProperties.KEY_ALGORITHM_RSA
        private const val BLOCK_MODE = KeyProperties.BLOCK_MODE_ECB
        private const val PADDING = KeyProperties.ENCRYPTION_PADDING_RSA_PKCS1
        private const val TRANSFORMATION = "$RSA_ALGORITHM/$BLOCK_MODE/$PADDING"
    }

    fun generateDHKeyPair(): KeyPair {
        val keyPairGenerator = KeyPairGenerator.getInstance(DH_ALGORITHM)
        keyPairGenerator.initialize(512, SecureRandom())
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

    @RequiresApi(Build.VERSION_CODES.O)
    fun generateSecret(publicSecret: String) {
        CoroutineScope(Dispatchers.IO).launch {
            val pubKey = java.util.Base64.getDecoder().decode(publicSecret)
            val keySpec = X509EncodedKeySpec(pubKey)
            val keyFactory = KeyFactory.getInstance("DH")
            val publicKey = keyFactory.generatePublic(keySpec)

            val privateSecret = SharedPreferences.read("DHPrivate", "")
            val priKey = java.util.Base64.getDecoder().decode(privateSecret)
            val keySpecPrivate = PKCS8EncodedKeySpec(priKey)
            val keyFactoryPrivate = KeyFactory.getInstance("DH")
            val privateKey = keyFactoryPrivate.generatePrivate(keySpecPrivate)

            val clientKeyAgreement: KeyAgreement = KeyAgreement.getInstance("DH")
            clientKeyAgreement.init(privateKey)
            clientKeyAgreement.doPhase(publicKey, true)
            val clientSharedSecret: ByteArray = clientKeyAgreement.generateSecret()

            SharedPreferences.write(
                "DHSecret",
                java.util.Base64.getEncoder().encodeToString(clientSharedSecret)
            )
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun encrypt(data: String, publicKey: String): String {
        val key = java.util.Base64.getDecoder().decode(publicKey)

        val keySpec = X509EncodedKeySpec(key)
        val keyFactory = KeyFactory.getInstance("RSA")
        val pubKey = keyFactory.generatePublic(keySpec)

        val cipher: Cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, pubKey)
        val bytes = cipher.doFinal(data.toByteArray())
        return Base64.encodeToString(bytes, Base64.DEFAULT)
    }

    fun decrypt(data: String): String {
        val cipher: Cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, getAsymmetricKeyPair().private)
        val encryptedData = Base64.decode(data, Base64.DEFAULT)
        val decodedData = cipher.doFinal(encryptedData)
        return String(decodedData)
    }
}