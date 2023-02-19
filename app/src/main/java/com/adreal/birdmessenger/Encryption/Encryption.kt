package com.adreal.birdmessenger.Encryption

import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.SecureRandom

class Encryption {

    companion object{
        const val ALGORITHM = "DH"
    }

    fun generateKeyPair() : KeyPair{
        val keyPairGenerator = KeyPairGenerator.getInstance(ALGORITHM)
        keyPairGenerator.initialize(2048, SecureRandom())
        return keyPairGenerator.generateKeyPair()
    }
}