package com.example.aiticketservice.client.config;

import io.netty.channel.ChannelOption;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.http.codec.json.Jackson2JsonDecoder;
import org.springframework.http.codec.json.Jackson2JsonEncoder;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;

@Configuration
public class QwenWebClientConfig {

    @Bean
    public WebClient qwenWebClient(AppProperties appProperties, com.fasterxml.jackson.databind.ObjectMapper objectMapper) {
        AppProperties.Qwen q = appProperties.getQwen();
        HttpClient httpClient = HttpClient.create()
                .responseTimeout(q.getTimeout())
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, (int) Math.min(q.getTimeout().toMillis(), Integer.MAX_VALUE));

        ExchangeStrategies strategies = ExchangeStrategies.builder()
                .codecs(configurer -> {
                    configurer.defaultCodecs().jackson2JsonEncoder(new Jackson2JsonEncoder(objectMapper));
                    configurer.defaultCodecs().jackson2JsonDecoder(new Jackson2JsonDecoder(objectMapper));
                })
                .build();

        return WebClient.builder()
                .baseUrl(trimTrailingSlash(q.getBaseUrl()))
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .exchangeStrategies(strategies)
                .build();
    }

    private static String trimTrailingSlash(String url) {
        if (url == null || url.isEmpty()) {
            return "";
        }
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }
}
