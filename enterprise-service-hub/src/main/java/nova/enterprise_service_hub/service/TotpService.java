package nova.enterprise_service_hub.service;

import dev.samstevens.totp.code.*;
import dev.samstevens.totp.exceptions.QrGenerationException;
import dev.samstevens.totp.qr.QrData;
import dev.samstevens.totp.qr.ZxingPngQrGenerator;
import dev.samstevens.totp.secret.DefaultSecretGenerator;
import dev.samstevens.totp.time.SystemTimeProvider;
import org.springframework.stereotype.Service;

import java.util.Base64;

/**
 * TOTP (Time-based One-Time Password) Service — Phase 6: 2FA/MFA
 * <p>
 * Generates TOTP secrets and validates codes for Google Authenticator.
 */
@Service
public class TotpService {

    private final DefaultSecretGenerator secretGenerator = new DefaultSecretGenerator(32);
    private final CodeVerifier verifier;

    public TotpService() {
        this.verifier = new DefaultCodeVerifier(
                new DefaultCodeGenerator(HashingAlgorithm.SHA1),
                new SystemTimeProvider()
        );
    }

    /**
     * Generate a new TOTP secret for a user.
     */
    public String generateSecret() {
        return secretGenerator.generate();
    }

    /**
     * Generate a QR code data URI for the TOTP secret.
     */
    public String generateQrCodeDataUri(String secret, String email) throws QrGenerationException {
        QrData data = new QrData.Builder()
                .label(email)
                .secret(secret)
                .issuer("Enterprise Service Hub")
                .algorithm(HashingAlgorithm.SHA1)
                .digits(6)
                .period(30)
                .build();

        ZxingPngQrGenerator generator = new ZxingPngQrGenerator();
        byte[] imageData = generator.generate(data);
        return "data:image/png;base64," + Base64.getEncoder().encodeToString(imageData);
    }

    /**
     * Verify a TOTP code against the user's secret.
     */
    public boolean verifyCode(String secret, String code) {
        return verifier.isValidCode(secret, code);
    }
}
