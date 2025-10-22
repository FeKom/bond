package github.fekom.bond.resolver;

import jakarta.servlet.http.HttpServletRequest;

public class ClientIPResolver {
    private static final String[] HEADERS_TO_TRY = {
        "X-Forwarded-For",
        "X-Real-IP", 
        "Proxy-Client-IP",
        "WL-Proxy-Client-IP",
        "HTTP_X_FORWARDED_FOR",
        "HTTP_X_FORWARDED",
        "HTTP_X_CLUSTER_CLIENT_IP",
        "HTTP_CLIENT_IP",
        "HTTP_FORWARDED_FOR",
        "HTTP_FORWARDED",
        "HTTP_VIA",
        "REMOTE_ADDR"
    };
    public String getClientIPAddress(HttpServletRequest request) {
        for (String header : HEADERS_TO_TRY) {
            var ip = request.getHeader(header); 
            if(ip != null && ip.length() != 0 && !"unknown".equalsIgnoreCase(ip)) {
                return ip;
            }
        }
        return request.getRemoteAddr();
    }
    private boolean isValidIP(String ip) {
      return ip != null && 
        ip.length() != 0 && 
            !"unknown".equalsIgnoreCase(ip) &&
            !"0:0:0:0:0:0:0:1".equals(ip) &&
            !"127.0.0.1".equals(ip);
    }
    
    private String extractFirstIP(String ipList) {
      // X-Forwarded-For pode ter: "client, proxy1, proxy2"
      if (ipList.contains(",")) {
        return ipList.split(",")[0].trim();
      }
      return ipList.trim();
    }

    public String getEnhancedIdentifier(HttpServletRequest request) {
    String ip = getClientIPAddress(request);
    String userAgent = request.getHeader("User-Agent");
    String userAgentHash = userAgent != null ? 
        String.valueOf(userAgent.hashCode() & 0xffff) : "unknown";
    
    return ip + "_" + userAgentHash;
    }

    private String normalizeIP(String ip) {
    if (ip.contains(":")) {
        // IPv6 - simplifica para evitar variações
        return "ipv6_" + Integer.toHexString(ip.hashCode() & 0xffff);
    }
    return ip;
    }

    public String getStableIdentifier(HttpServletRequest request) {
    String sessionId = request.getSession().getId();
    String ip = getClientIPAddress(request);
    
    return ip + "_" + sessionId.substring(0, 8);
    }
    
}
