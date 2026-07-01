# Circuit Breaker Pattern

A circuit breaker trips open after repeated failures to a dependency, short-circuiting further calls so the system fails fast. After a cool-down it allows a trial request and closes again once the dependency recovers.
