# Bearer Token Authentication

Bearer token authentication carries a secret in the Authorization header on every request. The server resolves the token to a principal; a stateless API keeps no session, so a revoked token stops working on the very next call.
