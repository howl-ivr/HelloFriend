package com.hellofriend.compartido;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

/** Activa {@code @Async}: los correos se envían en segundo plano para no hacer esperar la respuesta HTTP. */
@Configuration
@EnableAsync
public class AsincronoConfig {

}
