package com.commercehub.security;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;

@AutoConfiguration
@ComponentScan(basePackageClasses = JwtService.class)
@EnableConfigurationProperties(JwtProperties.class)
public class CommonSecurityAutoConfiguration {
}
