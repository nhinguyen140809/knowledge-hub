package com.knowledgehub.system.domain;

/** Reachability of a dependency, normalized from actuator's health {@code Status}. */
public enum DependencyState {
  UP,
  DOWN,
  /** No health contributor registered under that name, or a status this maps to neither. */
  UNKNOWN
}
