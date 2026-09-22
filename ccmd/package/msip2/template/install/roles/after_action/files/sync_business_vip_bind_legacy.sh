#!/bin/bash
# 老升新平台：为未绑定的业务服务（含后续新装业务包）绑定迁移后的 VIP
set +e

VIP_CFG="${VIP_CFG:-/data/commonConfig/vipConfig/vipConfig.conf}"
BIND_CFG="${BIND_CFG:-/data/commonConfig/vipConfig/serviceBindConfig.conf}"
LEGACY_VIP_FILE="${LEGACY_VIP_FILE:-/data/VirtualIP/virtualIP}"
BACKUP_META="${BACKUP_META:-/data/install/backup/saas/vip/.backup_meta}"
BACKUP_MARKER="${BACKUP_MARKER:-/data/install/backup/saas/vip/.backup_completed}"
LEGACY_VIP_BACKUP="${LEGACY_VIP_BACKUP:-/data/install/backup/saas/vip/legacy/VirtualIP}"
INSTALL_ROOT="${INSTALL_ROOT:-/data/install}"
VIP_SCAN_REL="${VIP_SCAN_REL:-conf/vip}"
BIND_API="${BIND_API:-https://127.0.0.1:7777/msiptool/vipConfig/bindVip}"
EXCLUDE_APPS="${EXCLUDE_APPS:-saas paas maintainer apaas}"
CURRENT_APP="${CURRENT_APP:-}"
BIND_RETRY="${BIND_RETRY:-3}"
MSIPTOOL_WAIT="${MSIPTOOL_WAIT:-30}"

log() {
  echo "[sync_business_vip_bind] $*"
}

wait_msiptool_ready() {
  local base_url="${BIND_API%/bindVip}"
  local i code
  for i in $(seq 1 "${MSIPTOOL_WAIT}"); do
    code=$(curl -k -s -o /dev/null -w "%{http_code}" "${base_url}/networkPort" 2>/dev/null)
    if [ "${code}" = "200" ]; then
      return 0
    fi
    sleep 2
  done
  return 1
}

is_msip_om_bound() {
  python3 - << PYEOF
import re
from pathlib import Path

bind_cfg = Path("${BIND_CFG}")
if not bind_cfg.is_file():
    raise SystemExit(1)
kv = re.compile(r"(\w+):(?:'(.*?)'|([^,]+))")
for line in bind_cfg.read_text(encoding="utf-8").splitlines():
    data = {}
    for m in kv.finditer(line.strip()):
        data[m.group(1)] = (m.group(2) if m.group(2) is not None else m.group(3)).strip()
    if data.get("serviceType") == "msip-om" and data.get("bind") == "true" and data.get("vip"):
        raise SystemExit(0)
raise SystemExit(1)
PYEOF
}

read_msip_om_vip_port() {
  python3 - << PYEOF
import re
from pathlib import Path

bind_cfg = Path("${BIND_CFG}")
kv = re.compile(r"(\w+):(?:'(.*?)'|([^,]+))")
vip, port = "", ""
if bind_cfg.is_file():
    for line in bind_cfg.read_text(encoding="utf-8").splitlines():
        data = {}
        for m in kv.finditer(line.strip()):
            data[m.group(1)] = (m.group(2) if m.group(2) is not None else m.group(3)).strip()
        if data.get("serviceType") == "msip-om" and data.get("bind") == "true":
            vip = data.get("vip", "")
            port = data.get("networkPort", "")
            break
print(f"{vip}|{port}")
PYEOF
}

is_legacy_migrated_platform() {
  if [ -f "${BACKUP_META}" ] && grep -E 'has_legacy:[[:space:]]*true' "${BACKUP_META}" >/dev/null 2>&1; then
    return 0
  fi
  if [ -f "${BACKUP_MARKER}" ] && { [ -d "${LEGACY_VIP_BACKUP}" ] || [ -f "${LEGACY_VIP_FILE}" ]; }; then
    if is_msip_om_bound; then
      return 0
    fi
  fi
  return 1
}

api_bind_success() {
  RESP_JSON="${1}" python3 - << 'PYEOF'
import json
import os
import sys

text = os.environ.get("RESP_JSON", "").strip()
if not text:
    sys.exit(1)
try:
    obj = json.loads(text)
except Exception:
    sys.exit(0 if text.lower() == "true" else 1)

# MSIPTool 包装响应：resultCode + datas（不是 data）
datas = obj.get("datas", obj.get("data"))
if datas is True:
    sys.exit(0)
code = str(obj.get("resultCode", obj.get("code", "")))
if code.endswith("000000") and datas is True:
    sys.exit(0)
sys.exit(1)
PYEOF
}

if ! is_legacy_migrated_platform; then
  log "skip: not legacy-migrated platform"
  exit 0
fi

if ! is_msip_om_bound; then
  log "skip: msip-om not bound yet, wait for saas vip migration"
  exit 0
fi

if [ -z "${CURRENT_APP}" ]; then
  log "WARNING: CURRENT_APP empty, skip bind to avoid touching unrelated apps"
  exit 0
fi

if ! wait_msiptool_ready; then
  log "WARNING: MSIPTool vip API not ready, skip business vip bind"
  exit 0
fi

OM_VIP_PORT="$(read_msip_om_vip_port)"
TARGET_VIP="${OM_VIP_PORT%%|*}"
TARGET_PORT="${OM_VIP_PORT#*|}"

if [ -z "${TARGET_VIP}" ] && [ -f "${VIP_CFG}" ]; then
  TARGET_PORT="$(grep -o 'networkPort:[^,]*' "${VIP_CFG}" | head -1 | cut -d: -f2- | tr -d '[:space:]')"
  TARGET_VIP="$(grep -o 'vip:[^,]*' "${VIP_CFG}" | head -1 | cut -d: -f2- | tr -d '[:space:]')"
fi
if [ -z "${TARGET_VIP}" ] && [ -f "${LEGACY_VIP_FILE}" ]; then
  TARGET_VIP="$(tr -d '[:space:]' < "${LEGACY_VIP_FILE}")"
fi
if [ -z "${TARGET_VIP}" ] && [ -f "${LEGACY_VIP_BACKUP}/virtualIP" ]; then
  TARGET_VIP="$(tr -d '[:space:]' < "${LEGACY_VIP_BACKUP}/virtualIP")"
fi
if [ -z "${TARGET_VIP}" ]; then
  log "skip: target vip not found"
  exit 0
fi
if [ -z "${TARGET_PORT}" ]; then
  TARGET_PORT="$(grep -o 'networkPort:[^,]*' "${VIP_CFG}" 2>/dev/null | head -1 | cut -d: -f2- | tr -d '[:space:]')"
fi
if [ -z "${TARGET_PORT}" ]; then
  TARGET_PORT="$(ls -l /sys/class/net/*/device 2>/dev/null | awk -F/ '{print $5}' | head -1)"
fi
TARGET_PORT="${TARGET_PORT:-eth0}"

mkdir -p "$(dirname "${BIND_CFG}")"
if [ ! -f "${BIND_CFG}" ]; then
  touch "${BIND_CFG}"
fi

log "scope: current app=${CURRENT_APP}, target vip=${TARGET_VIP}, port=${TARGET_PORT}"

export TARGET_VIP TARGET_PORT BIND_CFG INSTALL_ROOT VIP_SCAN_REL EXCLUDE_APPS CURRENT_APP
SERVICES="$(python3 - << 'PYEOF'
import os
import re
from pathlib import Path

bind_cfg = Path(os.environ["BIND_CFG"])
install_root = Path(os.environ["INSTALL_ROOT"])
scan_rel = os.environ["VIP_SCAN_REL"]
exclude = set(x for x in os.environ.get("EXCLUDE_APPS", "").split() if x)
current_app = os.environ.get("CURRENT_APP", "").strip()
target_vip = os.environ["TARGET_VIP"]
pattern = re.compile(r"^svc_vip_(.+)\.(ya?ml)\.j2$")
kv_pattern = re.compile(r"(\w+):(?:'(.*?)'|([^,]+))")

def parse_line(line):
    data = {}
    for m in kv_pattern.finditer(line.strip()):
        data[m.group(1)] = (m.group(2) if m.group(2) is not None else m.group(3)).strip()
    return data

def format_line(data, order=("appName", "serviceType", "bind", "networkPort", "vip", "remarks")):
    parts = []
    for key in order:
        if key in data:
            parts.append(f"{key}:{data[key]}")
    for key, val in data.items():
        if key not in order:
            parts.append(f"{key}:{val}")
    return ",".join(parts)

lines = []
if bind_cfg.is_file():
    lines = [ln.rstrip("\n") for ln in bind_cfg.read_text(encoding="utf-8").splitlines() if ln.strip()]

index = {}
updated = []
for line in lines:
    data = parse_line(line)
    app = data.get("appName", "")
    st = data.get("serviceType", "")
    if app and st:
        index[(app, st)] = data
    updated.append(format_line(data) if data else line)

discovered = set()
app_dir = install_root / current_app
vip_dir = app_dir / scan_rel
if vip_dir.is_dir():
    for f in sorted(vip_dir.iterdir()):
        m = pattern.match(f.name)
        if m:
            discovered.add((current_app, m.group(1)))

for (app_name, service_type), data in list(index.items()):
    if app_name in exclude or app_name == "saas":
        continue
    if app_name != current_app:
        continue
    if service_type.startswith("msip-"):
        continue
    discovered.add((app_name, service_type))

for app_name, service_type in sorted(discovered):
    key = (app_name, service_type)
    tpl = app_dir / scan_rel / f"svc_vip_{service_type}.yaml.j2"
    if not tpl.is_file():
        alt = app_dir / scan_rel / f"svc_vip_{service_type}.yml.j2"
        if not alt.is_file() and key not in index:
            continue
    if key not in index:
        index[key] = {
            "appName": app_name,
            "serviceType": service_type,
            "bind": "false",
            "networkPort": "",
            "vip": "",
            "remarks": "",
        }
        updated.append(format_line(index[key]))

bind_cfg.write_text("\n".join(updated) + ("\n" if updated else ""), encoding="utf-8")

to_bind = []
for app_name, service_type in sorted(discovered):
    data = index.get((app_name, service_type), {})
    if data.get("bind") == "true" and data.get("vip") == target_vip:
        continue
    to_bind.append(f"{app_name}\t{service_type}")

print("\n".join(to_bind))
PYEOF
)"

if [ -z "${SERVICES}" ]; then
  log "no unbound business vip services to bind for app=${CURRENT_APP}"
  if [ ! -d "${INSTALL_ROOT}/${CURRENT_APP}/${VIP_SCAN_REL}" ]; then
    log "WARNING: vip template dir missing: ${INSTALL_ROOT}/${CURRENT_APP}/${VIP_SCAN_REL}"
  fi
  exit 0
fi

bind_vip_api() {
  local app_name="$1"
  local service_type="$2"
  local tpl="${INSTALL_ROOT}/${app_name}/${VIP_SCAN_REL}/svc_vip_${service_type}.yaml.j2"
  local payload resp attempt ok

  if [ ! -f "${tpl}" ] && [ ! -f "${INSTALL_ROOT}/${app_name}/${VIP_SCAN_REL}/svc_vip_${service_type}.yml.j2" ]; then
    log "WARNING: vip template missing : ${tpl}"
    return 1
  fi

  payload=$(APP_NAME="${app_name}" SERVICE_TYPE="${service_type}" TARGET_PORT="${TARGET_PORT}" TARGET_VIP="${TARGET_VIP}" python3 - << 'PYEOF'
import json
import os
print(json.dumps({
    "confName": "serviceBindConfig",
    "appName": os.environ["APP_NAME"],
    "serviceType": os.environ["SERVICE_TYPE"],
    "bind": "false",
    "networkPort": os.environ["TARGET_PORT"],
    "vip": os.environ["TARGET_VIP"],
    "remarks": ""
}))
PYEOF
)
  ok=1
  for attempt in $(seq 1 "${BIND_RETRY}"); do
    resp=$(curl -k -s -S -X POST -H "Content-Type: application/json" -d "${payload}" "${BIND_API}" 2>&1)
    if api_bind_success "${resp}"; then
      ok=0
      break
    fi
    sleep 2
  done
  if [ "${ok}" -ne 0 ]; then
    log "WARNING: bind failed app=${app_name} serviceType=${service_type} resp=${resp}"
    return 1
  fi
  log "bind succeeded app=${app_name} serviceType=${service_type}"
  return 0
}

bind_ok=0
bind_fail=0
while IFS=$'\t' read -r app_name service_type; do
  [ -z "${app_name}" ] && continue
  [ -z "${service_type}" ] && continue
  log "binding ${app_name}/${service_type} vip=${TARGET_VIP} port=${TARGET_PORT}"
  if bind_vip_api "${app_name}" "${service_type}"; then
    bind_ok=$((bind_ok + 1))
  else
    bind_fail=$((bind_fail + 1))
  fi
done <<EOF
${SERVICES}
EOF

if [ "${bind_fail}" -gt 0 ]; then
  log "WARNING: ${bind_fail} business vip bind failed, ${bind_ok} succeeded"
  exit 0
fi

log "completed: bound ${bind_ok} business vip service(s)"
exit 0
