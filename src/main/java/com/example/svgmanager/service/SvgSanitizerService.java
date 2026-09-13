package com.example.svgmanager.service;

public interface SvgSanitizerService {

    byte[] sanitizeAndValidateSvg(byte[] rawSvgBytes);
}
