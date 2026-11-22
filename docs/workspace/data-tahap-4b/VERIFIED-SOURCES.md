# Verified public contact seed

Reviewed on 2026-11-16 against the official-domain URLs stored on every row in
`verified_numbers_seed.json`. These are public organizational service contacts,
not personal contacts. `verified_by` records an automated official-domain review;
it does not claim a human reviewer. Verification expires on 2027-05-16 and is not
renewed by server restart or seed import. Recheck the linked source before renewal.

The seed covers Halo BCA (telephone and WhatsApp), BRI, Mandiri, BNI, BSI,
OJK, Kring Pajak, Bea Cukai, PLN, both BPJS services, Pos, JNE, J&T, Shopee,
OVO and DANA. The exact supporting official URL is retained on each row.

Bank service numbers such as 1500888 are national service numbers, not E.164
numbers. Preserve them without a country/area prefix. BCA explicitly warns that
prefixed variants are impersonation risks:
https://www.bca.co.id/id/informasi/awas-modus/2026/04/13/11/06/waspada-nomor-halo-bca-palsu-di-website-instansi
The seed does not grant verification to +621500888 or +62211500888.
Shopee's official help page lists +62 21 1500702 for outbound account/service
calls and says it is not a complaint hotline; the label retains that distinction.

Tokopedia Care WhatsApp is deliberately omitted: official-domain searches did
not establish a current public WhatsApp number, and https://www.tokopedia.com/help
was inaccessible during review. Search results contained conflicting third-party
numbers, which are not sufficient evidence for granting a verification shield.
Use Tokopedia's in-app help pending verifiable official-domain evidence.

A verified contact identifies the published number; it cannot authenticate a
caller's identity in the presence of caller-ID spoofing.
