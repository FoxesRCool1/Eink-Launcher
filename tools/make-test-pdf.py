#!/usr/bin/python3
"""Writes a small PDF for trying the PDF reader in the emulator.

Twelve US Letter pages of numbered lines with wide white margins, so zoom,
"Crop the margins" and "Go to page" all have something to show. It uses only
the fonts every PDF reader has built in, and nothing but the standard library.

    /usr/bin/python3 tools/make-test-pdf.py build/samples/Test-pages.pdf
"""

import sys

PAGES = 12
WIDTH, HEIGHT = 612, 792
MARGIN = 110  # wide on purpose: the margin crop needs margins to find


def page_stream(number: int) -> bytes:
    lines = [f"BT /F2 20 Tf {MARGIN} {HEIGHT - MARGIN} Td (Test page {number} of {PAGES}) Tj ET"]
    y = HEIGHT - MARGIN - 40
    row = 1
    while y > MARGIN:
        text = f"Page {number}, line {row}. Circle this word with the pen, then change the zoom."
        lines.append(f"BT /F1 11 Tf {MARGIN} {y} Td ({text}) Tj ET")
        y -= 18
        row += 1
    # A frame around the text block, so a crop that cuts into the text shows.
    lines.append(f"0.5 w {MARGIN - 8} {MARGIN - 8} {WIDTH - 2 * MARGIN + 16} {HEIGHT - 2 * MARGIN + 16} re S")
    return "\n".join(lines).encode("latin-1")


def build() -> bytes:
    objects = []  # bytes of each object body, index 0 is object 1

    def add(body: bytes) -> int:
        objects.append(body)
        return len(objects)

    catalog = add(b"")  # filled in below
    pages = add(b"")
    font_body = add(b"<< /Type /Font /Subtype /Type1 /BaseFont /Times-Roman >>")
    font_head = add(b"<< /Type /Font /Subtype /Type1 /BaseFont /Times-Bold >>")

    kids = []
    for number in range(1, PAGES + 1):
        stream = page_stream(number)
        content = add(b"<< /Length %d >>\nstream\n" % len(stream) + stream + b"\nendstream")
        page = add(
            (
                f"<< /Type /Page /Parent {pages} 0 R /MediaBox [0 0 {WIDTH} {HEIGHT}] "
                f"/Resources << /Font << /F1 {font_body} 0 R /F2 {font_head} 0 R >> >> "
                f"/Contents {content} 0 R >>"
            ).encode("latin-1")
        )
        kids.append(page)

    objects[catalog - 1] = f"<< /Type /Catalog /Pages {pages} 0 R >>".encode("latin-1")
    kid_refs = " ".join(f"{kid} 0 R" for kid in kids)
    objects[pages - 1] = f"<< /Type /Pages /Count {PAGES} /Kids [{kid_refs}] >>".encode("latin-1")

    out = bytearray(b"%PDF-1.4\n%\xe2\xe3\xcf\xd3\n")
    offsets = []
    for index, body in enumerate(objects, start=1):
        offsets.append(len(out))
        out += f"{index} 0 obj\n".encode("latin-1") + body + b"\nendobj\n"

    xref = len(out)
    out += f"xref\n0 {len(objects) + 1}\n".encode("latin-1")
    out += b"0000000000 65535 f \n"
    for offset in offsets:
        out += f"{offset:010d} 00000 n \n".encode("latin-1")
    out += (
        f"trailer\n<< /Size {len(objects) + 1} /Root {catalog} 0 R >>\nstartxref\n{xref}\n%%EOF\n"
    ).encode("latin-1")
    return bytes(out)


if __name__ == "__main__":
    target = sys.argv[1] if len(sys.argv) > 1 else "Test-pages.pdf"
    with open(target, "wb") as handle:
        handle.write(build())
    print(f"Wrote {target}")
