package io.terbium.ossi

import com.sun.org.apache.xml.internal.security.utils.Base64
import java.security.SecureRandom
import java.security.spec.KeySpec
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

val random = SecureRandom()

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
    return Base64.encode(bytes)
}
