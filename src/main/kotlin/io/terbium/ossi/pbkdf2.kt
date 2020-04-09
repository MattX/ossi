package io.terbium.ossi

import java.security.SecureRandom
import java.security.spec.KeySpec
import java.util.*
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

private val random = SecureRandom()
private val b64enc = Base64.getEncoder()

// TODO don't do this
fun pbkdf2(of: String): String {
    val salt = ByteArray(16)
    val spec: KeySpec = PBEKeySpec(of.toCharArray(), salt, 65536, 128)
    val f = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1")
    val hash = f.generateSecret(spec).encoded

    return hash.take(6).joinToString("") { String.format("%02X", it) }
}

fun getAuthenticationToken(): String {
    val bytes = ByteArray(32)
    random.nextBytes(bytes)
    return b64enc.encodeToString(bytes)
}
