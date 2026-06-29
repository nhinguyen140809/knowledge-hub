package com.knowledgehub.knowledge.ingestion.application;

import com.knowledgehub.shared.error.DomainException;
import com.knowledgehub.shared.error.ErrorCode;

/** Thrown when an operation references a source id that does not exist. Maps to HTTP 404. */
public class SourceNotFoundException extends DomainException {

  public SourceNotFoundException(String sourceId) {
    super(ErrorCode.SOURCE_NOT_FOUND, "Source not found: " + sourceId);
  }
}
