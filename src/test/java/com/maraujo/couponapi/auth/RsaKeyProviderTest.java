package com.maraujo.couponapi.auth;

import static org.assertj.core.api.Assertions.*;

import com.maraujo.couponapi.config.security.RsaKeyProvider;
import java.util.Base64;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;

class RsaKeyProviderTest {
  ByteArrayResource pem(String type, byte[] encoded) {
    return new ByteArrayResource(
        ("-----BEGIN "
                + type
                + "-----\n"
                + Base64.getMimeEncoder().encodeToString(encoded)
                + "\n-----END "
                + type
                + "-----")
            .getBytes());
  }

  @Test
  void loadsMatchingPemKeys() throws Exception {
    var generated = RsaKeyProvider.generate();
    var loaded =
        RsaKeyProvider.load(
            pem("PRIVATE KEY", generated.toRSAPrivateKey().getEncoded()),
            pem("PUBLIC KEY", generated.toRSAPublicKey().getEncoded()));
    assertThat(loaded.toRSAPublicKey().getModulus())
        .isEqualTo(generated.toRSAPublicKey().getModulus());
  }

  @Test
  void rejectsMissingOrMismatchedKeys() throws Exception {
    assertThatThrownBy(() -> RsaKeyProvider.load(null, null))
        .isInstanceOf(IllegalStateException.class);
    var first = RsaKeyProvider.generate();
    var second = RsaKeyProvider.generate();
    assertThatThrownBy(
            () ->
                RsaKeyProvider.load(
                    pem("PRIVATE KEY", first.toRSAPrivateKey().getEncoded()),
                    pem("PUBLIC KEY", second.toRSAPublicKey().getEncoded())))
        .isInstanceOf(IllegalStateException.class);
    assertThatThrownBy(
            () ->
                RsaKeyProvider.load(pem("PRIVATE KEY", first.toRSAPrivateKey().getEncoded()), null))
        .isInstanceOf(IllegalStateException.class);
  }
}
