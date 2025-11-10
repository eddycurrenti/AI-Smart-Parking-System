

import cv2
import json
import datetime
import os
from ultralytics import YOLO

MODEL_PATH = "my_model.pt"
FRAME_PATH = "static/parking.jpg"
OUTPUT_IMG_PATH = "output/annotated_output.jpg"
OUTPUT_JSON_PATH = "output/occupancy.json"
HANDICAP_SPOT_ID = "spot_1"

if os.path.exists(OUTPUT_JSON_PATH):
    with open(OUTPUT_JSON_PATH, "r") as f:
        old_data = json.load(f)
    old_spots = {s["id"]: s for s in old_data.get("spots", [])}
else:
    old_data, old_spots = {}, {}

print("Loading YOLO model...")
model = YOLO(MODEL_PATH)
frame = cv2.imread(FRAME_PATH)
if frame is None:
    raise FileNotFoundError(f"Could not find image at {FRAME_PATH}")

results = model(frame, verbose=False)[0]
labels = model.names

spots_data, entry_points = [], []

spot_counter = 0
for box in results.boxes:
    cls_id = int(box.cls[0])
    cls_name = labels[cls_id].lower()
    conf = float(box.conf[0])
    x1, y1, x2, y2 = map(int, box.xyxy[0].tolist())

    if "entry" in cls_name:
        entry_points.append({
            "label": cls_name,
            "bbox": [x1, y1, x2, y2],
            "confidence": round(conf, 2)
        })
        continue

    
    if "occupied" in cls_name:
        state = "occupied"
    elif "free" in cls_name:
        state = "free"
    else:
        continue

    spot_counter += 1
    spot_id = f"spot_{spot_counter}"

   
    if spot_id in old_spots:
        prev_state = old_spots[spot_id]["state"]
        state = prev_state  
        
    if spot_id == HANDICAP_SPOT_ID:
        state = old_spots.get(spot_id, {}).get("state", "free")

    spots_data.append({
        "id": spot_id,
        "state": state,
        "bbox": [x1, y1, x2, y2],
        "confidence": round(conf, 2)
    })

for spot in spots_data:
    x1, y1, x2, y2 = spot["bbox"]
    state = spot["state"]
    spot_id = spot["id"]

    if spot_id == HANDICAP_SPOT_ID:
        color = (255, 0, 0)  
    elif state == "free":
        color = (0, 255, 0)
    else:
        color = (0, 0, 255)

    cv2.rectangle(frame, (x1, y1), (x2, y2), color, 2)

    label = f"{spot_id} ({state.upper()})"
    if spot_id == HANDICAP_SPOT_ID:
        label += "RESERVED"

    size = cv2.getTextSize(label, cv2.FONT_HERSHEY_SIMPLEX, 0.6, 2)[0]
    tx, ty = x1 + 5, y1 + size[1] + 5
    cv2.rectangle(frame, (x1, y1), (x1 + size[0] + 10, y1 + size[1] + 10), color, -1)
    text_color = (0, 0, 0) if state == "free" else (255, 255, 255)
    cv2.putText(frame, label, (tx, ty), cv2.FONT_HERSHEY_SIMPLEX, 0.6, text_color, 2)


total_free = sum(1 for s in spots_data if s["state"] == "free")
total_occupied = sum(1 for s in spots_data if s["state"] == "occupied")
total_reserved = 1

cv2.rectangle(frame, (10, 10), (360, 110), (30, 30, 30), -1)
cv2.putText(frame, f"Free: {total_free}", (20, 40), cv2.FONT_HERSHEY_SIMPLEX, 0.7, (0, 255, 0), 2)
cv2.putText(frame, f"Occupied: {total_occupied}", (20, 70), cv2.FONT_HERSHEY_SIMPLEX, 0.7, (0, 0, 255), 2)
cv2.putText(frame, f"Reserved: {total_reserved}", (200, 70), cv2.FONT_HERSHEY_SIMPLEX, 0.7, (255, 0, 0), 2)


output_data = {
    "timestamp": datetime.datetime.now().isoformat(),
    "entry_points": entry_points,
    "spots": spots_data
}
cv2.imwrite(OUTPUT_IMG_PATH, frame)
with open(OUTPUT_JSON_PATH, "w") as f:
    json.dump(output_data, f, indent=2)

print(f"Annotated image saved to {OUTPUT_IMG_PATH}")
print(f"occupancy.json merged & saved to {OUTPUT_JSON_PATH}")
print(f"Total Spots: {len(spots_data)} | Free: {total_free} | Occupied: {total_occupied}")

try:
    cv2.imshow("Updated Parking Map", frame)
    print("🖼️ Press 'q' to close preview window.")
    while True:
        if cv2.waitKey(1) & 0xFF == ord('q'):
            break
except:
    pass
finally:
    cv2.destroyAllWindows()

print("Done — persistent annotation completed successfully.")

