package io.terbium.ossi

import java.security.SecureRandom
import java.security.spec.KeySpec
import java.util.*
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

private val random: SecureRandom = SecureRandom.getInstanceStrong()


// TODO don't do this
fun pbkdf2(of: String): String {
    val salt = ByteArray(16)
    random.nextBytes(salt)
    val spec: KeySpec = PBEKeySpec(of.toCharArray(), salt, 65536, 128)
    val f = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1")
    val hash = f.generateSecret(spec).encoded
    val enc: Base64.Encoder = Base64.getEncoder()
    return "${enc.encodeToString(salt)}:${enc.encodeToString(hash)}"
}
