package com.example.svgmanager.service.internal.v2;

import com.example.svgmanager.entity.v2.ClientSvgShare;

public interface ClientShareTokenService {

    String generateToken(ClientSvgShare share, long validitySeconds);

    boolean validateToken(String token, ClientSvgShare share);

    long getDefaultTokenValiditySeconds();
}
