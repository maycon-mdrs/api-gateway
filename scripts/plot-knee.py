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

# Knee de vazão TCP/HTTP=5; usable demo=8; degradação (Error>1%)=15
KNEE_THR = 5
USABLE = 8
DEGRADE = 15


def mark_tcp_http(ax, legend=False):
    ax.axvline(KNEE_THR, color="#e17055", linestyle="--", linewidth=1.2,
               label="Knee vazão ≈ 5" if legend else None)
    ax.axvline(USABLE, color="#0984e3", linestyle="--", linewidth=1.2,
               label="Usable = 8" if legend else None)
    ax.axvline(DEGRADE, color="#d63031", linestyle=":", linewidth=1.2,
               label="Knee qualidade ≈ 15" if legend else None)
    ax.grid(True, alpha=0.3)


def series(proto, key):
    pts = data[proto]
    return [x["users"] for x in pts], [x[key] for x in pts]


# TCP + HTTP: três painéis, vazão no eixo até ~40 req/s
fig, axes = plt.subplots(3, 1, figsize=(9, 10), dpi=150, sharex=True)
for p in ("TCP", "HTTP"):
    xs, thr = series(p, "thr")
    _, avg = series(p, "avg")
    _, err = series(p, "err")
    axes[0].plot(xs, thr, "o-", label=p, color=colors[p], linewidth=2, markersize=5)
    axes[1].plot(xs, avg, "o-", label=p, color=colors[p], linewidth=2, markersize=5)
    axes[2].plot(xs, err, "o-", label=p, color=colors[p], linewidth=2, markersize=5)
for i, ax in enumerate(axes):
    mark_tcp_http(ax, legend=(i == 0))
axes[0].set_ylim(0, 40)
axes[0].set_ylabel("Throughput (req/s)")
axes[0].set_title("TCP e HTTP — Knee e Usable (EC2 us-east-2, 2 BR + 2 PT)")
axes[0].legend(loc="upper right", fontsize=8)
axes[1].set_ylabel("Response time (ms)")
axes[1].legend(loc="upper left", fontsize=8)
axes[2].set_xlabel("Load (users)")
axes[2].set_ylabel("Error %")
axes[2].legend(loc="upper left", fontsize=8)
fig.tight_layout()
fig.savefig(out_dir / "knee-tcp-http.png")
plt.close(fig)

# Mesmas curvas de vazão e latência, arquivos usados como figura isolada
fig, ax = plt.subplots(figsize=(9, 5), dpi=150)
for p in ("TCP", "HTTP"):
    xs, thr = series(p, "thr")
    ax.plot(xs, thr, "o-", label=p, color=colors[p], linewidth=2, markersize=5)
mark_tcp_http(ax, legend=True)
ax.set_ylim(0, 40)
ax.set_xlabel("Load (users)")
ax.set_ylabel("Throughput (req/s)")
ax.set_title("Throughput vs Load — TCP e HTTP (remoto)")
ax.legend(loc="upper right", fontsize=8)
fig.tight_layout()
fig.savefig(out_dir / "knee-throughput.png")
plt.close(fig)

fig, ax = plt.subplots(figsize=(9, 5), dpi=150)
for p in ("TCP", "HTTP"):
    xs, avg = series(p, "avg")
    ax.plot(xs, avg, "o-", label=p, color=colors[p], linewidth=2, markersize=5)
mark_tcp_http(ax, legend=True)
ax.set_xlabel("Load (users)")
ax.set_ylabel("Response time médio (ms)")
ax.set_title("Response Time vs Load — TCP e HTTP (remoto)")
ax.legend(loc="upper left", fontsize=8)
fig.tight_layout()
fig.savefig(out_dir / "knee-latency.png")
plt.close(fig)

# UDP: vazão ainda sobe em 80 users; Error permanece baixo
fig, axes = plt.subplots(3, 1, figsize=(9, 10), dpi=150, sharex=True)
xs, thr = series("UDP", "thr")
_, avg = series("UDP", "avg")
_, err = series("UDP", "err")
axes[0].plot(xs, thr, "o-", color=colors["UDP"], linewidth=2, markersize=5, label="UDP")
axes[1].plot(xs, avg, "o-", color=colors["UDP"], linewidth=2, markersize=5, label="UDP")
axes[2].plot(xs, err, "o-", color=colors["UDP"], linewidth=2, markersize=5, label="UDP")
for ax in axes:
    ax.axvline(USABLE, color="#0984e3", linestyle="--", linewidth=1.2, label="Usable demo = 8")
    ax.grid(True, alpha=0.3)
axes[0].set_ylabel("Throughput (req/s)")
axes[0].set_title("UDP — carga remota (sem joelho de erro até 80 users)")
axes[0].legend(loc="upper left", fontsize=8)
axes[1].set_ylabel("Response time (ms)")
axes[1].legend(loc="upper right", fontsize=8)
axes[2].set_xlabel("Load (users)")
axes[2].set_ylabel("Error %")
axes[2].set_ylim(0, 2)
axes[2].legend(loc="upper right", fontsize=8)
fig.tight_layout()
fig.savefig(out_dir / "knee-udp.png")
plt.close(fig)

# Visão conjunta: vazão de UDP no eixo direito para o joelho de TCP/HTTP continuar legível
fig, (ax1, ax2, ax3) = plt.subplots(3, 1, figsize=(9, 10), dpi=150, sharex=True)
ax1b = ax1.twinx()
for p in ("TCP", "HTTP"):
    xs, thr = series(p, "thr")
    _, avg = series(p, "avg")
    _, err = series(p, "err")
    ax1.plot(xs, thr, "o-", label=p, color=colors[p], linewidth=2, markersize=4)
    ax2.plot(xs, avg, "o-", label=p, color=colors[p], linewidth=2, markersize=4)
    ax3.plot(xs, err, "o-", label=p, color=colors[p], linewidth=2, markersize=4)
xs, thr = series("UDP", "thr")
_, avg = series("UDP", "avg")
_, err = series("UDP", "err")
ax1b.plot(xs, thr, "o-", label="UDP", color=colors["UDP"], linewidth=2, markersize=4)
ax2.plot(xs, avg, "o-", label="UDP", color=colors["UDP"], linewidth=2, markersize=4)
ax3.plot(xs, err, "o-", label="UDP", color=colors["UDP"], linewidth=2, markersize=4)
for ax in (ax1, ax2, ax3):
    ax.axvline(KNEE_THR, color="#e17055", linestyle="--", linewidth=1, alpha=0.9)
    ax.axvline(USABLE, color="#0984e3", linestyle="--", linewidth=1.2)
    ax.axvline(DEGRADE, color="#d63031", linestyle=":", linewidth=1)
    ax.grid(True, alpha=0.3)
ax1.set_ylim(0, 40)
ax1.set_ylabel("TCP/HTTP (req/s)")
ax1b.set_ylabel("UDP (req/s)", color=colors["UDP"])
ax1.set_title("Knee / Usable — visão conjunta (eixo direito = UDP)")
h1, l1 = ax1.get_legend_handles_labels()
h2, l2 = ax1b.get_legend_handles_labels()
ax1.legend(h1 + h2, l1 + l2, loc="center right", fontsize=8)
ax2.set_ylabel("Response time (ms)")
ax2.legend(loc="upper left", fontsize=8)
ax3.set_xlabel("Load (users)")
ax3.set_ylabel("Error %")
ax3.legend(loc="upper left", fontsize=8)
fig.tight_layout()
fig.savefig(out_dir / "knee-usable-combined.png")
plt.close(fig)

print("OK", [p.name for p in out_dir.glob("knee*.png")])
