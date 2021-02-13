package com.driima.binance.util;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class RateLimiter {

    private final long interval;
    private final int maxExecutions;
    private final Map<String, Long> executions = Maps.newHashMap();

    public RateLimiter(long interval, TimeUnit timeUnit) {
        this(interval, timeUnit, 1);
    }

    public RateLimiter(long interval, TimeUnit timeUnit, int executions) {
        this.interval = timeUnit.toMillis(interval);
        this.maxExecutions = executions;
    }

    public boolean execute(String key) {
        executions.entrySet().removeIf(entry -> entry.getKey().equalsIgnoreCase(key) && entry.getValue() < System.currentTimeMillis());
        int executed = executions.size();

        if (executed >= maxExecutions) {
            return false;
        }

        executions.put(key, System.currentTimeMillis() + interval);

        return true;
    }
}
