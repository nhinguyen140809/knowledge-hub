package com.knowledgehub.system.domain.port;

import java.util.Optional;

/**
 * Stores the operator-editable system settings that must survive a restart — currently just the
 * display product name. Config supplies the seed value; once an operator sets a value here, the
 * stored one wins.
 */
public interface SystemSettings {

  /** The stored product-name override, or empty when none has been set. */
  Optional<String> productName();

  /** Sets (or replaces) the product-name override. */
  void setProductName(String productName);
}
