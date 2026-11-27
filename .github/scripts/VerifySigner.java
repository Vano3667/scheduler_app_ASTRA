import com.android.apksig.ApkVerifier;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.cert.X509Certificate;
import java.util.List;

/** Verifies the APK and compares public DER certificates, never private-key bytes. */
class VerifySigner {
    public static void main(String[] args) throws Exception {
        var result = new ApkVerifier.Builder(new File(args[0])).build().verify();
        if (!result.isVerified()) {
            throw new IllegalStateException("Подпись APK недействительна: " + result.getErrors());
        }
        List<X509Certificate> certs = result.getSignerCertificates();
        if (certs.size() != 1) throw new IllegalStateException("Число подписантов: " + certs.size());
        byte[] expected = Files.readAllBytes(Path.of(args[1]));
        byte[] actual = certs.get(0).getEncoded();
        if (!MessageDigest.isEqual(expected, actual)) throw new IllegalStateException("APK подписан не ключом ASTRA; публикация остановлена");
        System.out.println("Подпись действительна. Сертификат единственного подписанта совпал с сертификатом ASTRA.");
    }
}
