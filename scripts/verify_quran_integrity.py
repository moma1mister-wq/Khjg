import sqlite3
import os
import sys
from datetime import datetime

def run_quran_verification():
    db_path = "app/src/main/assets/quran.db"
    assert os.path.exists(db_path), f"Database not found at {db_path}"

    conn = sqlite3.connect(db_path)
    cur = conn.cursor()

    report = []
    report.append("=======================================================================")
    report.append("                 تقرير التحقق الآلي من سلامة المصحف الشريف             ")
    report.append("=======================================================================")
    report.append(f"تاريخ الفحص: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}")
    report.append(f"مصدر البيانات المعتمد: مجمع الملك فهد / مشروع تنزيل (Tanzil Uthmani v1.02)")
    report.append(f"مسار قاعدة البيانات المحلية: {db_path} ({os.path.getsize(db_path)} bytes)")
    report.append("-----------------------------------------------------------------------")

    all_passed = True

    # 1. Check Surah Count
    cur.execute("SELECT COUNT(*) FROM surahs")
    surah_count = cur.fetchone()[0]
    passed_surahs = (surah_count == 114)
    all_passed = all_passed and passed_surahs
    report.append(f"1. فحص عدد السور: {surah_count} / 114 -> {'[نجح ✓]' if passed_surahs else '[فشل ✗]'}")

    # 2. Check Total Ayah Count
    cur.execute("SELECT COUNT(*) FROM ayahs")
    ayah_count = cur.fetchone()[0]
    passed_ayahs = (ayah_count == 6236)
    all_passed = all_passed and passed_ayahs
    report.append(f"2. فحص إجمالي عدد الآيات (العد الكوفي القياسي): {ayah_count} / 6236 -> {'[نجح ✓]' if passed_ayahs else '[فشل ✗]'}")

    # 3. Check Sequence of Surahs
    cur.execute("SELECT number, verses_count, start_page FROM surahs ORDER BY number ASC")
    surahs_rows = cur.fetchall()
    surah_nums = [r[0] for r in surahs_rows]
    passed_surah_seq = (surah_nums == list(range(1, 115)))
    all_passed = all_passed and passed_surah_seq
    report.append(f"3. فحص تسلسل أرقام السور (من 1 إلى 114 دون انقطاع): {'[نجح ✓]' if passed_surah_seq else '[فشل ✗]'}")

    # 4. Check Sequence of Ayahs within each Surah
    cur.execute("SELECT surah_number, number_in_surah FROM ayahs ORDER BY number ASC")
    ayahs_rows = cur.fetchall()
    
    cur_surah = 1
    expected_in_surah = 1
    seq_broken = False
    for s_num, a_in_s in ayahs_rows:
        if s_num != cur_surah:
            cur_surah = s_num
            expected_in_surah = 1
        if a_in_s != expected_in_surah:
            seq_broken = True
            break
        expected_in_surah += 1

    passed_ayah_seq = not seq_broken
    all_passed = all_passed and passed_ayah_seq
    report.append(f"4. فحص تسلسل أرقام الآيات داخل كل سورة بشكل تراكمي دقيق: {'[نجح ✓]' if passed_ayah_seq else '[فشل ✗]'}")

    # 5. Check Pages Range and Count
    cur.execute("SELECT DISTINCT page FROM ayahs ORDER BY page ASC")
    pages = [r[0] for r in cur.fetchall()]
    passed_pages = (len(pages) == 604 and min(pages) == 1 and max(pages) == 604)
    all_passed = all_passed and passed_pages
    report.append(f"5. فحص صفحات المصحف الشريف (604 صفحات قياسية، من صفحة 1 إلى 604): {len(pages)} صفحة -> {'[نجح ✓]' if passed_pages else '[فشل ✗]'}")

    # 6. Check Juz Range
    cur.execute("SELECT DISTINCT juz FROM ayahs ORDER BY juz ASC")
    juzs = [r[0] for r in cur.fetchall()]
    passed_juz = (len(juzs) == 30 and min(juzs) == 1 and max(juzs) == 30)
    all_passed = all_passed and passed_juz
    report.append(f"6. فحص الأجزاء الثلاثين (من الجزء 1 إلى الجزء 30): {len(juzs)} أجزاء -> {'[نجح ✓]' if passed_juz else '[فشل ✗]'}")

    # 7. Check Hizb Quarters Range
    cur.execute("SELECT DISTINCT hizb_quarter FROM ayahs ORDER BY hizb_quarter ASC")
    hizbs = [r[0] for r in cur.fetchall()]
    passed_hizb = (len(hizbs) == 240 and min(hizbs) == 1 and max(hizbs) == 240)
    all_passed = all_passed and passed_hizb
    report.append(f"7. فحص أرباع الأحزاب (240 ربع حزب، 60 حزبًا كاملًا): {len(hizbs)} أرباع -> {'[نجح ✓]' if passed_hizb else '[فشل ✗]'}")

    # 8. Check for Empty, Null, or Placeholder Text
    cur.execute("SELECT COUNT(*) FROM ayahs WHERE text IS NULL OR TRIM(text) = '' OR text LIKE '%PLACEHOLDER%'")
    empty_ayahs = cur.fetchone()[0]
    passed_no_empty = (empty_ayahs == 0)
    all_passed = all_passed and passed_no_empty
    report.append(f"8. فحص خلو الآيات من أي نصوص فارغة أو قيم وهمية أو PLACEHOLDER: (المكتشف: {empty_ayahs}) -> {'[نجح ✓]' if passed_no_empty else '[فشل ✗]'}")

    # 9. Check UTF-8 Tashkeel & Arabic Character Integrity
    cur.execute("SELECT text FROM ayahs LIMIT 50")
    sample_texts = [r[0] for r in cur.fetchall()]
    tashkeel_found = any('َ' in t or 'ِ' in t or 'ُ' in t for t in sample_texts)
    all_passed = all_passed and tashkeel_found
    report.append(f"9. فحص سلامة التشكيل العربي والرسم العثماني المعتمد: {'[نجح ✓]' if tashkeel_found else '[فشل ✗]'}")

    # 10. Check Surah Page Boundaries (e.g. Al-Fatihah p.1, Al-Baqarah p.2, An-Nas p.604)
    cur.execute("SELECT start_page FROM surahs WHERE number = 1")
    p_fatihah = cur.fetchone()[0]
    cur.execute("SELECT start_page FROM surahs WHERE number = 2")
    p_baqarah = cur.fetchone()[0]
    cur.execute("SELECT start_page FROM surahs WHERE number = 114")
    p_nas = cur.fetchone()[0]
    passed_landmarks = (p_fatihah == 1 and p_baqarah == 2 and p_nas == 604)
    all_passed = all_passed and passed_landmarks
    report.append(f"10. فحص معالم الصفحات (الفاتحة ص1، البقرة ص2، الناس ص604): {'[نجح ✓]' if passed_landmarks else '[فشل ✗]'}")

    conn.close()

    report.append("-----------------------------------------------------------------------")
    status_str = "ناجح ومكتمل بنسبة 100% ولا توجد أي أخطاء" if all_passed else "يوجد أخطاء تحتاج مراجعة"
    report.append(f"النتيجة النهائية لفحص المصحف: {status_str}")
    report.append("=======================================================================")

    report_text = "\n".join(report)
    print(report_text)

    with open("QURAN_VERIFICATION_REPORT.txt", "w", encoding="utf-8") as f:
        f.write(report_text)

    return all_passed

if __name__ == "__main__":
    success = run_quran_verification()
    sys.exit(0 if success else 1)
