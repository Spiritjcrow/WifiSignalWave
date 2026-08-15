#!/data/data/com.termux/files/usr/bin/sh
set -eu
pkg install -y python termux-api
install -d "$HOME/.local/lib/wifisignalwave" "$HOME/.local/bin"
install -m 700 worker.py "$HOME/.local/lib/wifisignalwave/worker.py"
TOKEN="$(python -c 'import secrets; print(secrets.token_urlsafe(32))')"
umask 077
printf '%s\n' "$TOKEN" > "$HOME/.local/lib/wifisignalwave/token"
cat > "$HOME/.local/bin/wifisignalwave" <<'EOF'
#!/data/data/com.termux/files/usr/bin/sh
TOKEN="$(cat "$HOME/.local/lib/wifisignalwave/token")"
exec python "$HOME/.local/lib/wifisignalwave/worker.py" --token "$TOKEN"
EOF
chmod 700 "$HOME/.local/bin/wifisignalwave"
printf 'Installed. Pairing token (enter once in Android app):\n%s\nRun: ~/.local/bin/wifisignalwave\n' "$TOKEN"
