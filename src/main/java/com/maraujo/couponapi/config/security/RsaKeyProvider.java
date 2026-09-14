package com.maraujo.couponapi.config.security;

import com.nimbusds.jose.jwk.RSAKey;
import java.io.IOException;
import java.security.*;
import java.security.interfaces.*;
import java.util.UUID;
import org.springframework.core.io.Resource;
import org.springframework.security.converter.RsaKeyConverters;

public final class RsaKeyProvider {
  private RsaKeyProvider() {}

  public static RSAKey generate() throws GeneralSecurityException {
    var generator = KeyPairGenerator.getInstance("RSA");
    generator.initialize(2048);
    var pair = generator.generateKeyPair();
    return new RSAKey.Builder((RSAPublicKey) pair.getPublic())
        .privateKey((RSAPrivateKey) pair.getPrivate())
        .keyID(UUID.randomUUID().toString())
        .build();
  }

  public static RSAKey load(Resource privateKey, Resource publicKey) throws IOException {
    if (privateKey == null || publicKey == null) {
      throw new IllegalStateException(
          "Configure RSA private/public keys, or explicitly use the demo profile.");
    }
    try (var privateInput = privateKey.getInputStream();
        var publicInput = publicKey.getInputStream()) {
      var privateRsa = RsaKeyConverters.pkcs8().convert(privateInput);
      var publicRsa = RsaKeyConverters.x509().convert(publicInput);
      if (!privateRsa.getModulus().equals(publicRsa.getModulus())) {
        throw new IllegalStateException("RSA private/public keys do not match.");
      }
      return new RSAKey.Builder(publicRsa)
          .privateKey(privateRsa)
          .keyID(UUID.randomUUID().toString())
          .build();
    }
  }
}
