import zipfile
import os

# Search for Tier.class in build artifacts
found = False
for root, dirs, files in os.walk("build"):
    for f in files:
        if f.endswith(".jar"):
            jar = os.path.join(root, f)
            try:
                with zipfile.ZipFile(jar, 'r') as z:
                    if 'net/minecraft/world/item/Tier.class' in z.namelist():
                        print(f"Found Tier.class in: {jar}")
                        found = True
                        break
            except:
                pass
    if found:
        break

if not found:
    print("Tier.class not found in any jar")
