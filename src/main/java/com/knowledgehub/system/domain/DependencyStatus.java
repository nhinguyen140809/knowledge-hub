package com.knowledgehub.system.domain;

/** One dependency's reachability, keyed by the name shown in the UI. */
public record DependencyStatus(String name, DependencyState status) {}
