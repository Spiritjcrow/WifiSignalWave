#!/usr/bin/env python3
"""Authenticated loopback archive worker. Accepts only real Android observation records."""
import argparse, hashlib, hmac, json, os, sqlite3
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer

SCHEMA = """CREATE TABLE IF NOT EXISTS observations(
 observation_id TEXT PRIMARY KEY, captured_at_utc TEXT NOT NULL, source TEXT NOT NULL,
 results_updated INTEGER NOT NULL, ssid TEXT NOT NULL, bssid_sha256 TEXT NOT NULL,
 frequency_mhz INTEGER NOT NULL, rssi_dbm INTEGER NOT NULL, channel_width INTEGER NOT NULL,
 center_freq0_mhz INTEGER NOT NULL, device_api INTEGER NOT NULL, device_model TEXT NOT NULL,
 permission TEXT NOT NULL, interpretation TEXT NOT NULL, record_sha256 TEXT NOT NULL UNIQUE)"""

def validate_record(o):
    required = {"schemaVersion","observationId","capturedAtUtc","source","resultsUpdated","ssid",
      "bssidSha256","frequencyMhz","rssiDbm","channelWidth","centerFreq0Mhz","deviceApi",
      "deviceModel","permission","interpretation"}
    if set(o) != required: raise ValueError("record fields do not match production schema")
    if o["source"] != "android.wifi.ScanResult": raise ValueError("non-device source rejected")
    if len(o["bssidSha256"]) != 64: raise ValueError("identifier must be SHA-256 redacted")
    if "no bearing or exact distance" not in o["interpretation"]: raise ValueError("honest interpretation required")

class Handler(BaseHTTPRequestHandler):
    server_version = "WifiSignalWave/1"
    def auth(self):
        supplied = self.headers.get("Authorization", "")
        return hmac.compare_digest(supplied, "Bearer " + self.server.token)
    def send_json(self, code, data):
        raw=json.dumps(data,separators=(",",":")).encode(); self.send_response(code)
        self.send_header("Content-Type","application/json"); self.send_header("Content-Length",str(len(raw)))
        self.end_headers(); self.wfile.write(raw)
    def do_GET(self):
        if not self.auth(): return self.send_json(401,{"error":"unauthorized"})
        if self.path != "/v1/status": return self.send_json(404,{"error":"operation_not_allowed"})
        with sqlite3.connect(self.server.db) as db: count=db.execute("SELECT count(*) FROM observations").fetchone()[0]
        self.send_json(200,{"schemaVersion":1,"storedObservations":count})
    def do_POST(self):
        if not self.auth(): return self.send_json(401,{"error":"unauthorized"})
        if self.path != "/v1/import": return self.send_json(404,{"error":"operation_not_allowed"})
        try:
            length=int(self.headers.get("Content-Length","0"))
            if length < 1 or length > 10_000_000: raise ValueError("invalid content length")
            rows=[]
            for line in self.rfile.read(length).decode().splitlines():
                o=json.loads(line); validate_record(o)
                canonical=json.dumps(o,sort_keys=True,separators=(",",":")).encode()
                rows.append((*[o[k] for k in ["observationId","capturedAtUtc","source","resultsUpdated","ssid","bssidSha256","frequencyMhz","rssiDbm","channelWidth","centerFreq0Mhz","deviceApi","deviceModel","permission","interpretation"]],hashlib.sha256(canonical).hexdigest()))
            with sqlite3.connect(self.server.db) as db:
                db.execute(SCHEMA); db.executemany("INSERT OR IGNORE INTO observations VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",rows)
            self.send_json(200,{"accepted":len(rows)})
        except Exception as e: self.send_json(400,{"error":str(e)})
    def log_message(self, fmt, *args): pass

def main():
    p=argparse.ArgumentParser(); p.add_argument("--db",default=os.path.expanduser("~/.local/share/wifisignalwave/archive.db")); p.add_argument("--token",required=True); a=p.parse_args()
    if len(a.token)<24: raise SystemExit("token must contain at least 24 characters")
    os.makedirs(os.path.dirname(a.db),exist_ok=True)
    with sqlite3.connect(a.db) as db: db.execute(SCHEMA)
    server=ThreadingHTTPServer(("127.0.0.1",8765),Handler); server.token=a.token; server.db=a.db
    print("WifiSignalWave worker listening on 127.0.0.1:8765"); server.serve_forever()
if __name__ == "__main__": main()
