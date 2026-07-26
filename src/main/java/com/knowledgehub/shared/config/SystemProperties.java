package com.knowledgehub.shared.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Product branding, bound from {@code app.product-name}.
 *
 * @param productName the configured display name for the product; null/blank when unset (the
 *     service falls back to the technical application name). An operator's stored override — see
 *     {@code SystemSettings} — takes precedence over this.
 */
@Validated
@ConfigurationProperties("app")
public record SystemProperties(String productName) {}
