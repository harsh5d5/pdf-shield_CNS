package com.cns.project;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.PDSignature;
import org.bouncycastle.cms.CMSSignedData;
import org.bouncycastle.cms.SignerInformation;
import org.bouncycastle.cms.SignerInformationStore;
import org.bouncycastle.cms.jcajce.JcaSimpleSignerInfoVerifierBuilder;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.util.encoders.Hex;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.security.MessageDigest;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class PdfVerifierService {

    public Map<String, Object> verifyPdf(InputStream pdfStream) throws Exception {
        byte[] pdfBytes = pdfStream.readAllBytes();
        Map<String, Object> result = new HashMap<>();
        
        try (PDDocument document = Loader.loadPDF(pdfBytes)) {
            List<PDSignature> signatures = document.getSignatureDictionaries();
            
            if (signatures.isEmpty()) {
                result.put("isValid", false);
                result.put("message", "No digital signatures found in this PDF.");
                return result;
            }

            PDSignature signature = signatures.get(signatures.size() - 1);
            byte[] signatureContents = signature.getContents(pdfBytes);
            byte[] signedContent = signature.getSignedContent(pdfBytes);

            CMSSignedData cms = new CMSSignedData(
                new org.bouncycastle.cms.CMSProcessableByteArray(signedContent), 
                signatureContents
            );
            SignerInformationStore signers = cms.getSignerInfos();
            SignerInformation signer = signers.getSigners().iterator().next();

            // Get certificate
            Collection<X509CertificateHolder> certCollection = cms.getCertificates().getMatches(signer.getSID());
            X509CertificateHolder certHolder = certCollection.iterator().next();
            X509Certificate cert = new JcaX509CertificateConverter().setProvider("BC").getCertificate(certHolder);

            // STEP 1: Verify FIRST (this must happen before getContentDigest)
            boolean isSignatureValid = signer.verify(
                new JcaSimpleSignerInfoVerifierBuilder().setProvider("BC").build(cert)
            );

            // STEP 2: NOW we can get the content digest (original hash stored in signature)
            byte[] originalHashBytes = signer.getContentDigest();
            String originalHashHex = Hex.toHexString(originalHashBytes);

            // STEP 3: Calculate fresh hash from the signed content
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] freshHashBytes = digest.digest(signedContent);
            String freshHashHex = Hex.toHexString(freshHashBytes);

            result.put("isValid", isSignatureValid);
            result.put("originalHash", originalHashHex);
            result.put("freshHash", freshHashHex);
            result.put("signerName", signature.getName());
            result.put("signDate", signature.getSignDate() != null ? signature.getSignDate().getTime().toString() : "N/A");
            
            if (isSignatureValid) {
                result.put("message", "Verification Success: Hashes match perfectly!");
            } else {
                result.put("message", "Verification Failed: Document content has been altered!");
            }
            
            return result;
        } catch (Exception e) {
            e.printStackTrace();
            result.put("isValid", false);
            result.put("message", "Error: " + e.getMessage());
            return result;
        }
    }
}
