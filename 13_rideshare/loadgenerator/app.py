import random
import requests
import time
import threading
from collections import deque

from flask import Flask, jsonify, render_template_string
from flask_socketio import SocketIO

HOSTS = [
    "rideshare",
    "rideshare-lock",
    "rideshare-memory",
]

VEHICLES = [
    "bike",
    "scooter",
    "car",
]

app = Flask(__name__)
app.config["SECRET_KEY"] = "loadgen-secret"
socketio = SocketIO(app, cors_allowed_origins="*")

state_lock = threading.Lock()
load_running = False
logs = deque(maxlen=200)
service_started_at = time.strftime("%Y-%m-%d %H:%M:%S")


def is_running() -> bool:
    with state_lock:
        return load_running


def set_running(value: bool) -> None:
    global load_running
    with state_lock:
        load_running = value


def emit_status() -> None:
    socketio.emit("status", {"running": is_running()})


def log(message: str) -> None:
    timestamp = time.strftime("%Y-%m-%d %H:%M:%S")
    entry = f"[{timestamp}] {message}"
    print(entry, flush=True)
    logs.append(entry)
    socketio.emit("log", {"message": entry})
    emit_status()


def load_generator() -> None:
    log("Load generator thread started")
    log("Load generator is initially disabled")

    while True:
        if not is_running():
            time.sleep(0.5)
            continue

        host = random.choice(HOSTS)
        vehicle = random.choice(VEHICLES)

        log(f"requesting {vehicle} from {host}")

        try:
            resp = requests.get(f"http://{host}:5000/{vehicle}", timeout=2)
            resp.raise_for_status()
            log(f"received status={resp.status_code} url={resp.url}")
        except Exception as exc:
            log(f"http error {exc}")

        time.sleep(random.uniform(0.2, 0.4))


@app.route("/")
def index():
    html = """
    <!DOCTYPE html>
    <html lang="de">
    <head>
        <meta charset="UTF-8" />
        <meta name="viewport" content="width=device-width, initial-scale=1.0" />
        <title>Load Generator</title>
        <script src="https://cdn.socket.io/4.7.5/socket.io.min.js"></script>
        <style>
            body {
                font-family: Arial, sans-serif;
                margin: 2rem;
                background: #f4f6f8;
                color: #1f2937;
            }
            h1 {
                margin-bottom: 0.5rem;
            }
            .layout {
                max-width: 1000px;
            }
            .status-row {
                display: flex;
                align-items: center;
                gap: 1rem;
                margin: 1rem 0 1.5rem 0;
            }
            .badge {
                display: inline-block;
                padding: 0.45rem 0.8rem;
                border-radius: 999px;
                font-weight: bold;
                font-size: 0.95rem;
            }
            .running {
                background: #dcfce7;
                color: #166534;
            }
            .stopped {
                background: #fee2e2;
                color: #991b1b;
            }
            button {
                padding: 0.8rem 1.2rem;
                border: none;
                border-radius: 8px;
                cursor: pointer;
                font-size: 1rem;
                font-weight: bold;
                color: white;
            }
            .btn-start {
                background: #16a34a;
            }
            .btn-stop {
                background: #dc2626;
            }
            .panel {
                background: white;
                border-radius: 10px;
                padding: 1rem;
                box-shadow: 0 2px 10px rgba(0,0,0,0.08);
            }
            #logbox {
                background: #0f172a;
                color: #d1fae5;
                padding: 1rem;
                border-radius: 8px;
                height: 500px;
                overflow-y: auto;
                white-space: pre-wrap;
                font-family: Consolas, Monaco, monospace;
                font-size: 0.9rem;
                line-height: 1.4;
            }
            .hint {
                color: #6b7280;
                margin-bottom: 1rem;
            }
        </style>
    </head>
    <body>
        <div class="layout">
            <h1>Load Generator</h1>
            <div class="hint">Sendet zufällig verteilte Requests an die 3 rideshare services</div>

            <div class="status-row">
                <div>
                    Status:
                    <span id="statusBadge" class="badge {{ 'running' if running else 'stopped' }}">
                        {{ 'RUNNING' if running else 'STOPPED' }}
                    </span>
                </div>
                <button id="toggleBtn"
                        class="{{ 'btn-stop' if running else 'btn-start' }}"
                        onclick="toggleGenerator()">
                    {{ 'Stop' if running else 'Start' }}
                </button>
            </div>

            <div class="panel">
                <h2>Live Logs</h2>
                <div id="logbox">{% for line in logs %}{{ line }}
{% endfor %}</div>
            </div>
        </div>

        <script>
            const socket = io();
            const logbox = document.getElementById("logbox");
            const statusBadge = document.getElementById("statusBadge");
            const toggleBtn = document.getElementById("toggleBtn");

            function appendLog(line) {
                const needsNewline = logbox.textContent.length > 0 && !logbox.textContent.endsWith("\\n");
                logbox.textContent += (needsNewline ? "\\n" : "") + line;
                logbox.scrollTop = logbox.scrollHeight;
            }

            function updateStatus(running) {
                statusBadge.textContent = running ? "RUNNING" : "STOPPED";
                statusBadge.className = "badge " + (running ? "running" : "stopped");
                toggleBtn.textContent = running ? "Stop" : "Start";
                toggleBtn.className = running ? "btn-stop" : "btn-start";
            }

            socket.on("connect", () => {
                console.log("WebSocket connected");
            });

            socket.on("log", (data) => {
                appendLog(data.message);
            });

            socket.on("status", (data) => {
                updateStatus(data.running);
            });

            async function toggleGenerator() {
                const running = statusBadge.textContent === "RUNNING";
                const endpoint = running ? "/stop" : "/start";

                try {
                    const response = await fetch(endpoint, { method: "POST" });
                    const data = await response.json();
                    updateStatus(data.running);
                } catch (err) {
                    console.error("Toggle failed", err);
                }
            }
        </script>
    </body>
    </html>
    """
    return render_template_string(html, logs=list(logs), running=is_running())


@app.route("/start", methods=["POST"])
def start():
    if is_running():
        return jsonify({"status": "already running", "running": True})

    set_running(True)
    log("Load generator activated")
    return jsonify({"status": "started", "running": True})


@app.route("/stop", methods=["POST"])
def stop():
    if not is_running():
        return jsonify({"status": "already stopped", "running": False})

    set_running(False)
    log("Load generator stopped")
    return jsonify({"status": "stopped", "running": False})


@app.route("/health", methods=["GET"])
def health():
    return jsonify({
        "status": "up",
        "load_generator_running": is_running(),
        "service_started_at": service_started_at,
        "log_buffer_size": len(logs),
    }), 200


@socketio.on("connect")
def handle_connect():
    emit_status()
    for entry in logs:
        socketio.emit("log", {"message": entry})


if __name__ == "__main__":
    worker = threading.Thread(target=load_generator, daemon=True)
    worker.start()

    log("Starting load generator web service")
    log("Initial state: STOPPED")

    socketio.run(app, host="0.0.0.0", port=8080)