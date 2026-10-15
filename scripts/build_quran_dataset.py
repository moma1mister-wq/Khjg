import urllib.request
import json
import sqlite3
import os
import re
import sys

def normalize_arabic(text):
    # Strip Tashkeel & standard diacritics
    tashkeel = re.compile(r'[\u0617-\u061A\u064B-\u0652\u06D6-\u06ED\u0670\u0640]')
    s = tashkeel.sub('', text)
    s = s.replace('ٱ', 'ا').replace('إ', 'ا').replace('أ', 'ا').replace('آ', 'ا')
    s = s.replace('ة', 'ه').replace('ى', 'ي')
    return s.strip()

def build_quran_db():
    print("Fetching authentic Tanzil Uthmani Quran dataset...")
    url = "https://api.alquran.cloud/v1/quran/quran-uthmani"
    req = urllib.request.Request(url, headers={'User-Agent': 'StudentCompanion/1.0'})
    with urllib.request.urlopen(req) as response:
        raw_json = json.loads(response.read().decode('utf-8'))

    surahs = raw_json['data']['surahs']
    edition = raw_json['data']['edition']
    print(f"Loaded edition: {edition.get('name')} ({edition.get('englishName')})")

    total_surahs = len(surahs)
    total_ayahs = sum(len(s['ayahs']) for s in surahs)
    print(f"Total Surahs: {total_surahs}")
    print(f"Total Ayahs: {total_ayahs}")

    assert total_surahs == 114, f"Expected 114 surahs, found {total_surahs}"
    assert total_ayahs == 6236, f"Expected 6236 ayahs, found {total_ayahs}"

    db_path = "app/src/main/assets/quran.db"
    if os.path.exists(db_path):
        os.remove(db_path)

    conn = sqlite3.connect(db_path)
    cur = conn.cursor()

    cur.execute("""
    CREATE TABLE surahs (
        number INTEGER PRIMARY KEY,
        name TEXT NOT NULL,
        full_name TEXT NOT NULL,
        english_name TEXT NOT NULL,
        verses_count INTEGER NOT NULL,
        revelation_type TEXT NOT NULL,
        start_page INTEGER NOT NULL
    )
    """)

    cur.execute("""
    CREATE TABLE ayahs (
        number INTEGER PRIMARY KEY,
        surah_number INTEGER NOT NULL,
        number_in_surah INTEGER NOT NULL,
        juz INTEGER NOT NULL,
        page INTEGER NOT NULL,
        hizb_quarter INTEGER NOT NULL,
        text TEXT NOT NULL,
        search_text TEXT NOT NULL
    )
    """)

    cur.execute("CREATE INDEX idx_ayahs_page ON ayahs(page)")
    cur.execute("CREATE INDEX idx_ayahs_surah ON ayahs(surah_number)")
    cur.execute("CREATE INDEX idx_ayahs_juz ON ayahs(juz)")
    cur.execute("CREATE INDEX idx_ayahs_search ON ayahs(search_text)")

    all_pages = set()
    global_ayah_counter = 0

    for s in surahs:
        s_num = s['number']
        s_name = s['name'].replace('سُورَةُ ', '').strip()
        s_full_name = s['name'].strip()
        s_english = s['englishName'].strip()
        s_verses = len(s['ayahs'])
        s_rev = 'مكية' if s['revelationType'] == 'Meccan' else 'مدنية'
        s_start_page = s['ayahs'][0]['page']

        cur.execute(
            "INSERT INTO surahs VALUES (?, ?, ?, ?, ?, ?, ?)",
            (s_num, s_name, s_full_name, s_english, s_verses, s_rev, s_start_page)
        )

        for a in s['ayahs']:
            global_ayah_counter += 1
            a_num = a['number']
            assert a_num == global_ayah_counter, f"Mismatched ayah number {a_num} vs {global_ayah_counter}"
            a_num_in_surah = a['numberInSurah']
            a_juz = a['juz']
            a_page = a['page']
            a_hizb = a['hizbQuarter']
            a_text = a['text'].strip()
            a_search = normalize_arabic(a_text)

            all_pages.add(a_page)

            cur.execute(
                "INSERT INTO ayahs VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                (a_num, s_num, a_num_in_surah, a_juz, a_page, a_hizb, a_text, a_search)
            )

    conn.commit()
    conn.close()

    assert len(all_pages) == 604, f"Expected 604 pages, found {len(all_pages)}"
    assert min(all_pages) == 1 and max(all_pages) == 604, f"Page bounds mismatch: {min(all_pages)} to {max(all_pages)}"

    file_size = os.path.getsize(db_path)
    print(f"SUCCESS: Quran SQLite DB generated at {db_path} ({file_size} bytes / {file_size / (1024*1024):.2f} MB)")
    print(f"Verified: 114 Surahs, 6236 Ayahs, 604 Pages (1..604), Juz 1..30.")

if __name__ == "__main__":
    build_quran_db()
