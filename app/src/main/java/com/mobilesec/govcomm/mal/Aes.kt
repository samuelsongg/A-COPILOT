import android.util.Base64
import io.github.c0nnor263.obfustringcore.ObfustringThis
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

@ObfustringThis
object AESEncryption {
    private const val TRANSFORMATION = "AES/CBC/PKCS5Padding"
    private const val ALGORITHM = "AES"
    private const val SECRET_KEY = "YOUR_SECRET_KEY"
    private const val IV_SIZE = 16
    fun encrypt(data: String): String {
        val secretKeySpec = SecretKeySpec(SECRET_KEY.toByteArray(Charsets.UTF_8), ALGORITHM)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        val iv = ByteArray(IV_SIZE).also { SecureRandom().nextBytes(it) }
        cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, IvParameterSpec(iv))
        val encrypted = cipher.doFinal(data.toByteArray(Charsets.UTF_8))
        val encryptedWithIv = iv + encrypted
        return Base64.encodeToString(encryptedWithIv, Base64.DEFAULT)
    }

    fun decrypt(encryptedData: String): String {
        val decodedData = Base64.decode(encryptedData, Base64.DEFAULT)
        val iv = decodedData.copyOfRange(0, IV_SIZE)
        val secretKeySpec = SecretKeySpec(SECRET_KEY.toByteArray(Charsets.UTF_8), ALGORITHM)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, IvParameterSpec(iv))
        val original = cipher.doFinal(decodedData, IV_SIZE, decodedData.size - IV_SIZE)
        return String(original)
    }
}