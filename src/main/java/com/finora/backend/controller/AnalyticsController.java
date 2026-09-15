package com.finora.backend.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final StringRedisTemplate redisTemplate;

    @GetMapping("/{accountId}")
    public Map<String, Double> getCategoryBreakdown(@PathVariable String accountId) {
        String key = "analytics:" + accountId;

        Map<Object, Object> raw = redisTemplate.opsForHash().entries(key);

        return raw.entrySet().stream()
                .collect(Collectors.toMap(
                        e -> e.getKey().toString(),
                        e -> Double.parseDouble(e.getValue().toString())
                ));
    }
}