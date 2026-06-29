package com.knowledgehub.knowledge.ingestion.application;

import com.knowledgehub.shared.error.DomainException;
import com.knowledgehub.shared.error.ErrorCode;

/** Thrown when registering a source whose id already exists. Maps to HTTP 409. */
public class DuplicateSourceException extends DomainException {

  public DuplicateSourceException(String sourceId) {
    super(ErrorCode.DUPLICATE_SOURCE, "Source already exists: " + sourceId);
  }
}
