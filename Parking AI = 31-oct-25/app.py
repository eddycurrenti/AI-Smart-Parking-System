# ============================================================
# app.py — AI Smart Parking Flask Backend (Car-Type Aware)
# ============================================================

from flask import Flask, jsonify, request
import subprocess, json, base64, os
from threading import Thread

app = Flask(__name__)

# ------------------------------------------------------------
# Configuration
# ------------------------------------------------------------
YOLO_SCRIPT = "detect_parking_auto.py"
OUTPUT_JSON = "output/occupancy.json"
ANNOTATED_IMG = "output/annotated_output.jpg"
HANDICAP_SPOTS = ["spot_1"]  # Reserved spots

# ------------------------------------------------------------
# Utility Functions
# ------------------------------------------------------------
def load_data():
    if not os.path.exists(OUTPUT_JSON):
        return {"error": "No occupancy.json found."}
    try:
        with open(OUTPUT_JSON, "r") as f:
            return json.load(f)
    except Exception as e:
        return {"error": str(e)}


def save_data(data):
    os.makedirs(os.path.dirname(OUTPUT_JSON), exist_ok=True)
    with open(OUTPUT_JSON, "w") as f:
        json.dump(data, f, indent=2)


def encode_image_to_base64(path):
    if not os.path.exists(path):
        return None
    with open(path, "rb") as f:
        return base64.b64encode(f.read()).decode("utf-8")


def run_yolo_async():
    """Run YOLO asynchronously after state change."""
    def _run():
        try:
            subprocess.run(["python", "-u", YOLO_SCRIPT], cwd=os.getcwd(), check=True)
            print("✅ YOLO re-annotation complete.")
        except Exception as e:
            print(f"⚠️ YOLO refresh failed: {e}")
    Thread(target=_run, daemon=True).start()


# ------------------------------------------------------------
# ROUTES
# ------------------------------------------------------------
@app.route("/")
def home():
    return jsonify({
        "message": "✅ AI Smart Parking Backend Running",
        "routes": ["/status", "/recommend?type=", "/confirm", "/reset"]
    })


@app.route("/status", methods=["GET"])
def status():
    """Return parking lot status."""
    data = load_data()
    if "spots" not in data:
        return jsonify({"error": "No parking data found."}), 404

    all_occupied = all(s["state"].lower() == "occupied" for s in data["spots"])
    data["is_full"] = all_occupied

    img_b64 = encode_image_to_base64(ANNOTATED_IMG)
    if img_b64:
        data["image"] = img_b64

    return jsonify(data)


@app.route("/recommend", methods=["GET"])
def recommend():
    """
    Recommend parking spot based on car type.
    Car Types:
        compact, suv, muv, sedan, luxury
    """
    data = load_data()
    if "spots" not in data:
        return jsonify({"error": "No parking data found."}), 404

    car_type = request.args.get("type", "compact").strip().lower()
    print(f"🚗 Recommend called for car type: {car_type}")

    free_spots = [
        s for s in data["spots"]
        if s["state"].lower() == "free" and s["id"].lower() not in [h.lower() for h in HANDICAP_SPOTS]
    ]
    if not free_spots:
        return jsonify({"message": "🚫 No free spots available."})

    # Helper to count neighbouring free spots
    def count_free_neighbors(idx):
        left = (idx > 0 and data["spots"][idx - 1]["state"].lower() == "free")
        right = (idx < len(data["spots"]) - 1 and data["spots"][idx + 1]["state"].lower() == "free")
        return int(left) + int(right)

    ranked = []
    for idx, s in enumerate(data["spots"]):
        if s["state"].lower() == "free" and s["id"].lower() not in HANDICAP_SPOTS:
            ranked.append((s, count_free_neighbors(idx)))

    entry_points = data.get("entry_points", [])
    if entry_points:
        ex, ey = entry_points[0]["bbox"][:2]
        ranked.sort(key=lambda t: ((t[0]["bbox"][0]-ex)**2 + (t[0]["bbox"][1]-ey)**2))

    selected = None

    if car_type in ["compact", "muv"]:
        selected = ranked[0][0]

    elif car_type in ["suv", "sedan"]:
        ranked.sort(key=lambda t: (-t[1], t[0]["bbox"][0]))
        for s, n in ranked:
            if n >= 1:
                selected = s
                break
        if not selected:
            selected = ranked[0][0]

    elif car_type == "luxury":
        for s, n in ranked:
            if n == 2:
                selected = s
                break
        if not selected:
            return jsonify({"message": "🚫 No premium spots with both neighbours free."})

    if not selected:
        selected = ranked[0][0]

    img_b64 = encode_image_to_base64(ANNOTATED_IMG)
    if img_b64:
        selected["image"] = img_b64
    selected["car_type"] = car_type

    print(f"💡 Recommended {selected['id']} for {car_type.title()}")
    return jsonify(selected)


@app.route("/confirm", methods=["POST"])
def confirm():
    """Mark or unmark parking spot as occupied/free."""
    try:
        body = request.get_json()
        spot_id = str(body.get("id", "")).strip().lower()
        confirm_flag = body.get("confirm", True)

        data = load_data()
        if "spots" not in data:
            return jsonify({"error": "No parking data found."}), 404

        if spot_id in [h.lower() for h in HANDICAP_SPOTS] and confirm_flag:
            return jsonify({"error": f"⚠️ {spot_id} is reserved for handicapped drivers only."}), 403

        found = False
        for s in data["spots"]:
            if s["id"].lower() == spot_id:
                s["state"] = "occupied" if confirm_flag else "free"
                found = True
                break

        if not found:
            return jsonify({"error": f"Spot {spot_id} not found."}), 404

        save_data(data)
        run_yolo_async()
        state = "occupied" if confirm_flag else "free"
        return jsonify({"success": True, "message": f"Spot {spot_id} marked as {state}."})

    except Exception as e:
        return jsonify({"error": str(e)}), 500


@app.route("/reset", methods=["POST"])
def reset():
    """Reset all spots to free."""
    data = load_data()
    if "spots" not in data:
        return jsonify({"error": "No parking data found."}), 404

    for s in data["spots"]:
        s["state"] = "free"
    save_data(data)
    run_yolo_async()
    return jsonify({"success": True, "message": "All spots reset to FREE."})


if __name__ == "__main__":
    print("🚀 Starting AI Smart Parking Flask Server...")
    app.run(host="127.0.0.1", port=5000, debug=True)
