package com.cns.project;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.PDSignature;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.SignatureInterface;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaCertStore;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.cms.*;
import org.bouncycastle.cms.jcajce.JcaSignerInfoGeneratorBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.operator.jcajce.JcaDigestCalculatorProviderBuilder;
import org.springframework.stereotype.Service;

import java.io.*;
import java.math.BigInteger;
import java.security.*;
import java.security.cert.X509Certificate;
import java.util.*;

@Service
public class PdfSignerService {

    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    public byte[] signPdf(InputStream pdfStream, String signerName) throws Exception {
        // 1. Generate a fresh RSA Key Pair
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(2048);
        KeyPair keyPair = keyGen.generateKeyPair();
        PrivateKey privateKey = keyPair.getPrivate();

        // 2. Create a Self-Signed Certificate (required by PDF standard)
        X509Certificate cert = generateSelfSignedCert(keyPair, signerName);

        // 3. Load the PDF
        byte[] pdfBytes = pdfStream.readAllBytes();
        try (PDDocument document = Loader.loadPDF(pdfBytes)) {

            // 4. Create the Signature Dictionary (the "box")
            PDSignature signature = new PDSignature();
            signature.setFilter(PDSignature.FILTER_ADOBE_PPKLITE);
            signature.setSubFilter(PDSignature.SUBFILTER_ADBE_PKCS7_DETACHED);
            signature.setName(signerName);
            signature.setLocation("India");
            signature.setReason("Digitally Signed - CNS Project");
            signature.setSignDate(Calendar.getInstance());

            // 5. The Signing Logic (creates a proper CMS/PKCS#7 container)
            SignatureInterface signingLogic = content -> {
                try {
                    // Build CMS Signed Data
                    CMSSignedDataGenerator gen = new CMSSignedDataGenerator();

                    ContentSigner signer = new JcaContentSignerBuilder("SHA256withRSA")
                            .setProvider("BC").build(privateKey);

                    gen.addSignerInfoGenerator(
                            new JcaSignerInfoGeneratorBuilder(
                                    new JcaDigestCalculatorProviderBuilder()
                                            .setProvider("BC").build()
                            ).build(signer, cert)
                    );

                    gen.addCertificates(new JcaCertStore(Collections.singletonList(cert)));

                    // Read the content to sign
                    byte[] data = content.readAllBytes();
                    CMSTypedData cmsData = new CMSProcessableByteArray(data);
                    CMSSignedData signedData = gen.generate(cmsData, false);

                    return signedData.getEncoded();
                } catch (Exception e) {
                    throw new IOException(e);
                }
            };

            // 6. Embed signature and save
            document.addSignature(signature, signingLogic);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            document.saveIncremental(out);
            return out.toByteArray();
        }
    }

    /**
     * Generates a self-signed X.509 certificate for embedding in the PDF.
     */
    private X509Certificate generateSelfSignedCert(KeyPair keyPair, String name) throws Exception {
        X500Name issuer = new X500Name("CN=" + name + ", O=CNS Project, L=India");
        BigInteger serial = BigInteger.valueOf(System.currentTimeMillis());

        Calendar notBefore = Calendar.getInstance();
        Calendar notAfter = Calendar.getInstance();
        notAfter.add(Calendar.YEAR, 1);

        JcaX509v3CertificateBuilder builder = new JcaX509v3CertificateBuilder(
                issuer, serial,
                notBefore.getTime(), notAfter.getTime(),
                issuer, keyPair.getPublic()
        );

        ContentSigner signer = new JcaContentSignerBuilder("SHA256withRSA")
                .setProvider("BC").build(keyPair.getPrivate());

        X509CertificateHolder holder = builder.build(signer);
        return new JcaX509CertificateConverter()
                .setProvider("BC").getCertificate(holder);
    }
}
