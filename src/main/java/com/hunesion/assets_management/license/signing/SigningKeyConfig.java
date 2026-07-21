package com.hunesion.assets_management.license.signing;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(SigningKeyProperties.class)
public class SigningKeyConfig {
}
