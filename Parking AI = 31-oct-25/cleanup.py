import os
import shutil

paths_to_clean = [
    "output",             
    "runs",               
    "__pycache__",        
    "demo1.avi",          
    "capture.png"         
]

files_to_remove = [
    "occupancy.json",
    "annotated_output.jpg",
    "annotated.jpg"
]

def delete_path(path):
    """Safely delete a file or directory."""
    if os.path.exists(path):
        if os.path.isdir(path):
            shutil.rmtree(path)
            print(f"Deleted folder: {path}")
        else:
            os.remove(path)
            print(f"Deleted file: {path}")
    else:
        print(f"Skipped (not found): {path}")

def main():
    print("\n🧹 Cleaning up generated files...\n")
    for path in paths_to_clean:
        delete_path(path)

    for f in files_to_remove:
        delete_path(f)
        
    os.makedirs("output", exist_ok=True)
    print("\n Cleanup complete! Project is reset and ready to run again.\n")

if __name__ == "__main__":
    main()
