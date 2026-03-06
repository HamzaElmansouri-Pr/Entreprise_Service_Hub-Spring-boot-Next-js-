package nova.enterprise_service_hub.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Payload for digitally signing a proposal via the public signing link.
 */
public record ProposalSignRequest(
        @NotBlank(message = "Signer name is required")
        @Size(max = 200)
        String signerName,

        @NotBlank(message = "Signer email is required")
        @Size(max = 200)
        String signerEmail,

        @NotBlank(message = "Signature data is required (base64 PNG)")
        String signatureData) {
}
