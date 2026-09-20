#!/usr/bin/python3
"""
A stand-in for GitHub Releases, to try the in-app update without a release.

It answers the two requests the app makes: the list of releases, and the
download of one file, which it redirects to a second port the way GitHub
redirects to its storage server. It also checks that the access token does
not follow the redirect.

Usage:
  tools/fake-github.py <apk file> <version> [port]

  apk file   the build to offer, for example a copy of app-viwoods-debug.apk
             built with a higher appVersionName
  version    the version to call it, for example 0.1.1
  port       8765 by default. The file is served from port + 1.

In the app, debug build: Settings, Help, "Design demo", Dev. Put
http://<this machine>:<port>/x into the build URL, press "Updates from the
build URL", then Settings, Help, "Check for updates". In the emulator this
machine is 10.0.2.2.
"""

import hashlib
import json
import sys
import threading
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer

if len(sys.argv) < 3:
    sys.exit(__doc__)

APK_PATH = sys.argv[1]
VERSION = sys.argv[2]
PORT = int(sys.argv[3]) if len(sys.argv) > 3 else 8765

with open(APK_PATH, "rb") as handle:
    APK = handle.read()
SHA256 = hashlib.sha256(APK).hexdigest()


def release_list(host: str) -> bytes:
    assets = []
    for index, flavour in enumerate(("viwoods", "generic"), start=1):
        assets.append({
            "id": index,
            "name": f"eink-launcher-v{VERSION}-{flavour}-debug.apk",
            "size": len(APK),
            "state": "uploaded",
            "digest": f"sha256:{SHA256}",
            "url": f"http://{host}/repos/test/releases/assets/{index}",
        })
    release = {
        "tag_name": f"v{VERSION}",
        "name": f"Eink Launcher v{VERSION}",
        "draft": False,
        "prerelease": False,
        "body": "- This release comes from tools/fake-github.py.\n- Nothing was sent to GitHub.",
        "assets": assets,
    }
    return json.dumps([release]).encode()


class Api(BaseHTTPRequestHandler):
    def do_GET(self):
        host = self.headers.get("Host", f"127.0.0.1:{PORT}")
        name = host.rsplit(":", 1)[0]
        if "/releases/assets/" in self.path:
            self.send_response(302)
            self.send_header("Location", f"http://{name}:{PORT + 1}/signed/file.apk?sig=test")
            self.end_headers()
        elif self.path.split("?")[0].endswith("/releases"):
            body = release_list(host)
            self.send_response(200)
            self.send_header("Content-Type", "application/json")
            self.send_header("Content-Length", str(len(body)))
            self.end_headers()
            self.wfile.write(body)
        else:
            self.send_error(404)

    def log_message(self, fmt, *args):
        token = "with a token" if self.headers.get("Authorization") else "no token"
        print(f"api      {self.command} {self.path}  ({token})")


class Storage(BaseHTTPRequestHandler):
    def do_GET(self):
        if self.headers.get("Authorization"):
            # The real storage server refuses this too.
            print("storage  REFUSED: the access token followed the redirect")
            self.send_error(400, "The token must not come here")
            return
        self.send_response(200)
        self.send_header("Content-Type", "application/vnd.android.package-archive")
        self.send_header("Content-Length", str(len(APK)))
        self.end_headers()
        self.wfile.write(APK)

    def log_message(self, fmt, *args):
        print(f"storage  {self.command} {self.path.split('?')[0]}")


print(f"Offering v{VERSION}, {len(APK)} bytes, sha256 {SHA256[:16]}...")
print(f"API on port {PORT}, file on port {PORT + 1}. Stop with Ctrl+C.")
threading.Thread(
    target=ThreadingHTTPServer(("0.0.0.0", PORT + 1), Storage).serve_forever, daemon=True
).start()
ThreadingHTTPServer(("0.0.0.0", PORT), Api).serve_forever()
