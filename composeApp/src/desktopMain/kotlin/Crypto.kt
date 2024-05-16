import com.github.javakeyring.Keyring
import com.github.javakeyring.PasswordAccessException
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.security.SecureRandom
import java.util.*
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.PBEParameterSpec

interface EncryptDecrypt {
    fun encrypt(data: ByteArray): ByteArray
    fun decrypt(data: ByteArray): ByteArray
}

private const val ITERATION_COUNT = 20
private const val SALT_SIZE = 16

class AesCipher(passphrase: String) : EncryptDecrypt {
    private val cipher = Cipher.getInstance("PBEWithMD5AndDES")
    private val key =
        SecretKeyFactory.getInstance("PEBWithMD5AndDES")
            .generateSecret(PBEKeySpec(passphrase.toCharArray()))
    private val salt =
        MessageDigest.getInstance("SHA-1")
            .digest(passphrase.toByteArray())
            .slice(0 until SALT_SIZE)
            .toByteArray()

    override fun encrypt(data: ByteArray): ByteArray {
        cipher.init(Cipher.ENCRYPT_MODE, key, PBEParameterSpec(salt, ITERATION_COUNT))
        val encrypted = cipher.doFinal(data)
        return Base64.getEncoder().encode(encrypted)
    }

    override fun decrypt(data: ByteArray): ByteArray {
        cipher.init(Cipher.DECRYPT_MODE, key, PBEParameterSpec(salt, ITERATION_COUNT))
        val decoded = Base64.getDecoder().decode(data)
        return cipher.doFinal(decoded)
    }
}

fun AesCipher(): AesCipher {
    val keyring = Keyring.create()
    val passphrase = try {
        keyring.getPassword("libcheck", "default")
    } catch (_: PasswordAccessException) {
        val random = try {
            SecureRandom.getInstanceStrong()
        } catch (_: NoSuchAlgorithmException) {
            SecureRandom()
        }
        val seed = random.generateSeed(32)
        Base64.getEncoder().encodeToString(seed).also {
            keyring.setPassword("libcheck", "default", it)
        }
    }
    return AesCipher(passphrase)
}