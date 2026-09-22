import csv
from pathlib import Path
import matplotlib
matplotlib.use("Agg")
import matplotlib.pyplot as plt

csv_path = Path("jmeter/knee-usable-template.csv")
raw = []
with csv_path.open(encoding="utf-8-sig") as f:
    for line in f:
        line = line.strip()
        if not line or line.startswith("#"):
            continue
        raw.append(line)

reader = csv.DictReader(raw)
data = {"TCP": [], "HTTP": [], "UDP": []}
for r in reader:
    proto = r["protocolo"].strip()
    if proto not in data:
        continue
    data[proto].append({
        "users": int(float(r["users"])),
        "thr": float(r["throughput"]),
        "avg": float(r["average_ms"]),
        "err": float(r["error_pct"]),
    })
for p in data:
    data[p].sort(key=lambda x: x["users"])

out_dir = Path("docs/figuras")
out_dir.mkdir(parents=True, exist_ok=True)
colors = {"TCP": "#1f77b4", "HTTP": "#2ca02c", "UDP": "#d62728"}

# Knee thr TCP/HTTP=5; usable demo=8; degradacao=15; UDP soft-knee~50
KNEE_THR = 5
USABLE = 8
DEGRADE = 15
UDP_SOFT = 50

fig, ax = plt.subplots(figsize=(9, 5), dpi=150)
for p, pts in data.items():
    ax.plot([x["users"] for x in pts], [x["thr"] for x in pts],
            "o-", label=p, color=colors[p], linewidth=2, markersize=5)
ax.axvline(KNEE_THR, color="#e17055", linestyle="--", linewidth=1.2, label=f"Knee thr TCP/HTTP (={KNEE_THR})")
ax.axvline(USABLE, color="#0984e3", linestyle="--", linewidth=1.2, label=f"Usable demo (={USABLE})")
ax.axvline(UDP_SOFT, color="#6c5ce7", linestyle=":", linewidth=1.2, label=f"Soft-knee UDP (~{UDP_SOFT})")
ax.axvspan(KNEE_THR, DEGRADE, color="#ffeaa7", alpha=0.25)
ax.set_xlabel("Load (users)")
ax.set_ylabel("Throughput (req/s)")
ax.set_title("Throughput vs Load — EC2 us-east-2 (remoto, 2BR+2PT)")
ax.grid(True, alpha=0.3)
ax.legend(loc="best", fontsize=8)
fig.tight_layout()
fig.savefig(out_dir / "knee-throughput.png")
plt.close(fig)

fig, ax = plt.subplots(figsize=(9, 5), dpi=150)
for p, pts in data.items():
    ax.plot([x["users"] for x in pts], [x["avg"] for x in pts],
            "o-", label=p, color=colors[p], linewidth=2, markersize=5)
ax.axvline(KNEE_THR, color="#e17055", linestyle="--", linewidth=1.2, label=f"Knee thr (={KNEE_THR})")
ax.axvline(USABLE, color="#0984e3", linestyle="--", linewidth=1.2, label=f"Usable (={USABLE})")
ax.axvline(DEGRADE, color="#d63031", linestyle=":", linewidth=1.2, label=f"Degradação Err>1% (~{DEGRADE})")
ax.set_xlabel("Load (users)")
ax.set_ylabel("Response time médio (ms)")
ax.set_title("Response Time vs Load — EC2 us-east-2 (remoto)")
ax.grid(True, alpha=0.3)
ax.legend(loc="best", fontsize=8)
fig.tight_layout()
fig.savefig(out_dir / "knee-latency.png")
plt.close(fig)

fig, (ax1, ax2, ax3) = plt.subplots(3, 1, figsize=(9, 10), dpi=150, sharex=True)
for p, pts in data.items():
    xs = [x["users"] for x in pts]
    ax1.plot(xs, [x["thr"] for x in pts], "o-", label=p, color=colors[p], linewidth=2, markersize=4)
    ax2.plot(xs, [x["avg"] for x in pts], "o-", label=p, color=colors[p], linewidth=2, markersize=4)
    ax3.plot(xs, [x["err"] for x in pts], "o-", label=p, color=colors[p], linewidth=2, markersize=4)
for ax in (ax1, ax2, ax3):
    ax.axvline(KNEE_THR, color="#e17055", linestyle="--", linewidth=1, alpha=0.9)
    ax.axvline(USABLE, color="#0984e3", linestyle="--", linewidth=1.2)
    ax.axvline(DEGRADE, color="#d63031", linestyle=":", linewidth=1)
    ax.grid(True, alpha=0.3)
ax1.annotate("Knee thr≈5", xy=(5.3, 120), fontsize=8, color="#e17055")
ax1.annotate("Usable=8", xy=(8.3, 200), fontsize=8, color="#0984e3")
ax1.annotate("UDP soft≈50", xy=(50.5, 350), fontsize=8, color="#6c5ce7")
ax1.set_ylabel("Throughput (req/s)")
ax1.set_title("Knee / Usable Capacity — API Gateway BR/PT na AWS (varredura 1–80)")
ax1.legend(loc="best", fontsize=8)
ax2.set_ylabel("Response time (ms)")
ax2.legend(loc="best", fontsize=8)
ax3.set_xlabel("Load (users)")
ax3.set_ylabel("Error %")
ax3.legend(loc="best", fontsize=8)
fig.tight_layout()
fig.savefig(out_dir / "knee-usable-combined.png")
plt.close(fig)

print("OK", [p.name for p in out_dir.glob("knee*.png")])
