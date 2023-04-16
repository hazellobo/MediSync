package info.neu.infoapp.config;

import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import lombok.SneakyThrows;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JWKConfig {
    @Bean
    @SneakyThrows
    public RSAKey rsaJWK() {
        return new RSAKeyGenerator(2048).keyID("123").generate();
    }

    @Bean
    @SneakyThrows
    public RSAKey rsaPublicKey(RSAKey rsaJWK) {
        return rsaJWK.toPublicJWK();
    }

}
