# AI-Smart-Parking-System
Parking lot spot suggestions using FLASK for backend,Yolo8V model trained and JSwing for UI or frontend
# 🅿️ AI Smart Parking System

An intelligent parking management system powered by **Flask** and **YOLO (You Only Look Once)** object detection.  

It automatically detects parking occupancy from images or video, recommends the best available spot, and updates parking status in real-time.

## 🚀 Features
- 🎯 **Automatic Parking Detection** using YOLOv8  
- 🧠 **Smart Recommendation System** — finds nearest free parking spot  
- 🗺️ **Annotated Output Image** — highlights free/occupied spots visually  
- ⚙️ **Flask API Backend** with endpoints for detection, recommendation & confirmation  
- 🧹 **Cleanup Script** to reset generated files  
- 🧰 **Modular Design** (Flask, Python, Java client support)

## 🧩 Project Structure
AI-Smart-Parking-System/
│

├── app.py # Flask backend API

├── detect_parking_auto.py # YOLO-based parking detection

├── cleanup.py # Utility to clean output and cache

├── test_flask.py # Quick test route for Flask

├── ParkingClient.java # Java client to interact with the Flask API

├── my_model.pt # Trained YOLO model (not included in GitHub)

├── json-20240303.jar # JSON library for Java client

├── static/

│ └── parking.jpg # Sample parking lot image

├── output/

│ ├── annotated_output.jpg # Annotated image after detection

│ └── occupancy.json # Parking data (free/occupied spots)

└── requirements.txt # Python dependencies


---

## ⚙️ Installation & Setup

### 1️⃣ Clone or Download the Project
If you don’t have Git installed, click the **“Code → Download ZIP”** button on GitHub and extract it.
If you have Git:
```bash
git clone https://github.com/YOUR_USERNAME/AI-Smart-Parking-System.git
cd AI-Smart-Parking-System

2️⃣ Create a Virtual Environment (optional but recommended)
python -m venv venv
venv\Scripts\activate   # for Windows
source venv/bin/activate  # for macOS/Linux

▶️ Run the System
🧠 Start the Flask Server
python app.py
Flask will start on:
http://127.0.0.1:5000/

🧾 API Routes
Endpoint	Method	Description
/	GET	Health check + available routes
/status	GET	Runs YOLO detection and returns current parking data
/recommend	GET	Suggests best available free spot
/confirm	POST	Marks a specific spot as occupied

🧪 Example Usage
Get Parking Status
curl http://127.0.0.1:5000/status
Get Recommended Spot
curl http://127.0.0.1:5000/recommend
Confirm a Spot as Occupied
curl -X POST http://127.0.0.1:5000/confirm \
     -H "Content-Type: application/json" \
     -d '{"id": "spot_3", "confirm": true}'
________________________________________

🧹 Cleanup Utility
To reset all generated outputs and clear the workspace:
python cleanup.py
This removes output/, cache folders, and recreates a clean output directory.

🧾 Requirements
List of key dependencies:
flask
ultralytics
opencv-python
torch
numpy

🧑‍💻 Author
Developed by: Tejas Patil
Project: AI Smart Parking System
Date: 2025
License: Private

📦 requirements.txt
# Core Framework
flask==3.0.3

# YOLOv8 and Computer Vision
ultralytics==8.2.90
opencv-python==4.10.0.84
torch>=2.3.0
torchvision>=0.18.0

# Utilities
numpy==1.26.4
Pillow==10.4.0
requests==2.32.3

# Optional (for JSON handling, used in ParkingClient.java)
jsonlib-python3==1.6.1




