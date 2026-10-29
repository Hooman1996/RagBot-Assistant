"""Build-only Maven cache for the server's blocked Google Maven route.

Serve Gradle on loopback only, after verifying downloads. Never packaged in the app.
Run from the activated assistant environment; logs and receipts stay project-local.
"""

import concurrent.futures
import hashlib
import json
import ssl
import threading
import urllib.error
import urllib.request
from collections import defaultdict
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2] / "local/scratch/replica-foundation"
CACHE = ROOT / "maven"
LOCK = threading.Lock()
FILE_LOCKS = defaultdict(threading.Lock)
CENTRAL = "https://repo.maven.apache.org/maven2/"
GOOGLE = "https://maven.aliyun.com/repository/google/"
INDEPENDENT = "https://repo.huaweicloud.com/repository/maven/"


def fetch(url):
    with urllib.request.urlopen(
        url, timeout=45, context=ssl.create_default_context(cafile="/etc/ssl/certs/ca-certificates.crt")
    ) as r:
        return r.read()


class Handler(BaseHTTPRequestHandler):
    def log_message(self, fmt, *args):
        pass

    def do_HEAD(self):
        with FILE_LOCKS[self.path]:
            self.respond(False)

    def do_GET(self):
        with FILE_LOCKS[self.path]:
            self.respond(True)

    def respond(self, body):
        path = self.path.lstrip("/")
        if ".." in path or "?" in path:
            self.send_error(400)
            return
        dest = CACHE / path
        try:
            if not dest.is_file():
                try:
                    data = fetch(CENTRAL + path)
                    origin = CENTRAL
                    if not path.endswith((".sha1", ".sha256", ".sha512", ".md5")):
                        expected = fetch(CENTRAL + path + ".sha1").decode().strip().split()[0]
                        assert hashlib.sha1(data).hexdigest() == expected
                except urllib.error.HTTPError as ex:
                    if ex.code != 404:
                        raise
                    data = fetch(GOOGLE + path)
                    origin = GOOGLE
                    if not path.endswith((".sha1", ".sha256", ".sha512", ".md5")):
                        with concurrent.futures.ThreadPoolExecutor(2) as pool:
                            checksum = pool.submit(fetch, INDEPENDENT + path + ".sha1")
                            copy = pool.submit(fetch, INDEPENDENT + path)
                            expected = checksum.result().decode().strip().split()[0]
                            independent = copy.result()
                        assert hashlib.sha1(data).hexdigest() == expected
                        assert len(data) == len(independent)
                        assert hashlib.sha256(data).digest() == hashlib.sha256(independent).digest()
                dest.parent.mkdir(parents=True, exist_ok=True)
                pending = dest.with_name(dest.name + ".pending")
                pending.write_bytes(data)
                pending.replace(dest)
                with LOCK, (ROOT / "maven-provenance.jsonl").open("a") as journal:
                    journal.write(
                        json.dumps(
                            dict(
                                path=path,
                                origin=origin,
                                size=len(data),
                                sha256=hashlib.sha256(data).hexdigest(),
                            )
                        )
                        + "\n"
                    )
            data = dest.read_bytes()
            self.send_response(200)
            self.send_header("Content-Length", str(len(data)))
            self.end_headers()
            if body:
                self.wfile.write(data)
        except (BrokenPipeError, ConnectionResetError):
            return
        except urllib.error.HTTPError as ex:
            self.send_error(ex.code)
        except Exception as ex:
            print(path, type(ex).__name__, str(ex), flush=True)
            self.send_error(502)


ThreadingHTTPServer(("127.0.0.1", 8766), Handler).serve_forever()
