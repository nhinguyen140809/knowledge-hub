package com.knowledgehub.system.infrastructure.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request to change the display product name — a free-form human label (unlike an id), so only
 * non-blank and a length bound are enforced.
 *
 * @param productName the new display name
 */
public record UpdateProductNameRequest(@NotBlank @Size(max = 100) String productName) {}
