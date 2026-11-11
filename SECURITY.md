# Security Policy

Sentinel is an academic/demo project and is not certified for operational forensic use.

## Reporting a vulnerability

Do not publish exploit details, credentials, private evidence, or personal data in a public issue. Contact the repository owner privately with a concise description, impact, and reproduction steps. A private reporting address has not been configured yet.

## Safe-use guidance

- Never commit evidence uploads, database dumps, credentials, tokens, or `.env` files.
- Use synthetic evidence and test accounts for demonstrations.
- Run the application on a trusted network unless it has been independently hardened.
- Treat generated hashes and custody records as application-level records, not as proof of legal admissibility or tamper-proof storage.
- Back up the database before applying schema changes.

## Important limitations

A SHA-256 match confirms that the bytes checked match the stored digest. It does not prove the authenticity or origin of the original evidence, nor does it protect against an attacker who can alter both the evidence and its recorded hash. Production use would require independent security review, protected audit records, secure key management, hardened access controls, and validated forensic procedures.
