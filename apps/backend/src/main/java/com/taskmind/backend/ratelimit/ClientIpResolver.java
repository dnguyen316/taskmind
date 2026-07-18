package com.taskmind.backend.ratelimit;

import jakarta.servlet.http.HttpServletRequest;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.List;

public class ClientIpResolver {

    public static final String MDC_KEY = "clientIp";
    private static final String X_FORWARDED_FOR = "X-Forwarded-For";

    private final RateLimitProperties properties;

    public ClientIpResolver(RateLimitProperties properties) {
        this.properties = properties;
    }

    public String resolve(HttpServletRequest request) {
        String remoteAddress = normalizeIp(request.getRemoteAddr());
        if (!isTrustedProxy(remoteAddress)) {
            return remoteAddress;
        }
        String forwardedClient = firstForwardedFor(request.getHeader(X_FORWARDED_FOR));
        if (forwardedClient == null) {
            return remoteAddress;
        }
        return forwardedClient;
    }

    private String firstForwardedFor(String header) {
        if (header == null || header.isBlank()) {
            return null;
        }
        return Arrays.stream(header.split(","))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .map(this::normalizeIp)
                .filter(value -> !value.isBlank())
                .findFirst()
                .orElse(null);
    }

    private boolean isTrustedProxy(String remoteAddress) {
        List<String> trustedProxies = properties.getTrustedProxies();
        if (trustedProxies == null || trustedProxies.isEmpty() || remoteAddress.isBlank()) {
            return false;
        }
        return trustedProxies.stream().anyMatch(proxy -> matches(proxy, remoteAddress));
    }

    private boolean matches(String proxy, String remoteAddress) {
        if (proxy == null || proxy.isBlank()) {
            return false;
        }
        String normalizedProxy = proxy.trim();
        if (normalizedProxy.contains("/")) {
            return cidrMatches(normalizedProxy, remoteAddress);
        }
        return normalizeIp(normalizedProxy).equals(remoteAddress);
    }

    private boolean cidrMatches(String cidr, String remoteAddress) {
        String[] parts = cidr.split("/", -1);
        if (parts.length != 2) {
            return false;
        }
        try {
            InetAddress network = InetAddress.getByName(stripBrackets(parts[0].trim()));
            InetAddress address = InetAddress.getByName(stripBrackets(remoteAddress));
            byte[] networkBytes = network.getAddress();
            byte[] addressBytes = address.getAddress();
            if (networkBytes.length != addressBytes.length) {
                return false;
            }
            int prefixLength = Integer.parseInt(parts[1]);
            if (prefixLength < 0 || prefixLength > networkBytes.length * Byte.SIZE) {
                return false;
            }
            int fullBytes = prefixLength / Byte.SIZE;
            int remainingBits = prefixLength % Byte.SIZE;
            for (int i = 0; i < fullBytes; i++) {
                if (networkBytes[i] != addressBytes[i]) {
                    return false;
                }
            }
            if (remainingBits == 0) {
                return true;
            }
            int mask = (0xFF << (Byte.SIZE - remainingBits)) & 0xFF;
            return (networkBytes[fullBytes] & mask) == (addressBytes[fullBytes] & mask);
        } catch (IllegalArgumentException | UnknownHostException e) {
            return false;
        }
    }

    private String normalizeIp(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        String candidate = stripBrackets(value.trim());
        int zoneIndex = candidate.indexOf('%');
        if (zoneIndex >= 0) {
            candidate = candidate.substring(0, zoneIndex);
        }
        try {
            return InetAddress.getByName(candidate).getHostAddress();
        } catch (UnknownHostException e) {
            return candidate;
        }
    }

    private String stripBrackets(String value) {
        if (value.startsWith("[") && value.endsWith("]")) {
            return value.substring(1, value.length() - 1);
        }
        return value;
    }
}
