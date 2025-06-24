import json
import re
from pathlib import Path
import requests
from bs4 import BeautifulSoup

# Αυτή η δέσμη ενεργειών κατεβάζει τις σελίδες Materialize Themes
# από ThemeForest και BootstrapMade και δημιουργεί αρχείο JSON
# με το όνομα και το βασικό χρώμα κάθε θέματος.

HEADERS = {
    "User-Agent": "Mozilla/5.0"
}

def fetch_themeforest_themes():
    url = "https://themeforest.net/category/site-templates/material-design"
    r = requests.get(url, headers=HEADERS)
    r.raise_for_status()
    soup = BeautifulSoup(r.text, "html.parser")
    items = []
    for item in soup.select("a.thumb"):
        name = item.get("title")
        if not name:
            continue
        name = name.strip()
        if not name:
            continue
        items.append({"label": name, "seed": "#2196F3"})
    return items

def fetch_bootstrapmade_themes():
    url = "https://bootstrapmade.com/bootstrap-template-categories/material-design/"
    r = requests.get(url, headers=HEADERS)
    r.raise_for_status()
    soup = BeautifulSoup(r.text, "html.parser")
    items = []
    for card in soup.select("div.item"):
        name_tag = card.select_one("h3")
        if not name_tag:
            continue
        name = name_tag.get_text(strip=True)
        items.append({"label": name, "seed": "#2196F3"})
    return items


def main():
    themes = []
    try:
        themes += fetch_themeforest_themes()
    except Exception as e:
        print(f"ThemeForest error: {e}")
    try:
        themes += fetch_bootstrapmade_themes()
    except Exception as e:
        print(f"BootstrapMade error: {e}")
    if themes:
        Path("app/src/main/assets").mkdir(parents=True, exist_ok=True)
        with open("app/src/main/assets/themes.json", "w", encoding="utf-8") as f:
            json.dump(themes, f, ensure_ascii=False, indent=2)
        print(f"Saved {len(themes)} themes to themes.json")
    else:
        print("Δεν βρέθηκαν θέματα")

if __name__ == "__main__":
    main()
