#!/usr/bin/env python3
"""Write actual Java/Maven and test evidence into README.md."""
from pathlib import Path
import re
root = Path(__file__).resolve().parents[1]
results = root / "results"
readme_path = root / "README.md"
readme = readme_path.read_text(encoding="utf-8")
tick = chr(96)
fence = tick * 3
def required(name):
    p = results / name
    if not p.is_file() or not p.read_text(encoding="utf-8", errors="replace").strip():
        raise SystemExit(f"Missing required result file: {name}")
    return p.read_text(encoding="utf-8", errors="replace").strip()
def summary(text):
    lines = [line.strip() for line in text.splitlines() if "Tests run:" in line]
    if not lines:
        raise SystemExit("Maven test summary missing")
    return lines[-1]
java = required("java-version.txt")
maven = required("mvn-version.txt")
green = summary(required("mvn-test.txt"))
if "Tests run: 10, Failures: 0, Errors: 0, Skipped: 0" not in green:
    raise SystemExit(f"Final Maven test count is not green: {green}")
env_section = (
    "### " + tick + "java -version" + tick + "\n\n" + fence + "text\n" + java + "\n" + fence + "\n\n"
    "### " + tick + "mvn -version" + tick + "\n\n" + fence + "text\n" + maven + "\n" + fence + "\n\n"
    f"Эцсийн тестийн summary: {tick}{green}{tick}\n"
)
env_start = "<!-- ENV-RESULTS-START -->"
env_end = "<!-- ENV-RESULTS-END -->"
if env_start not in readme or env_end not in readme:
    raise SystemExit("Environment markers missing from README")
pre, tail = readme.split(env_start, 1)
_, post = tail.split(env_end, 1)
readme = pre + env_start + "\n\n" + env_section + "\n" + env_end + post
mutant_start = "<!-- MUTATION-RESULTS-START -->"
mutant_end = "<!-- MUTATION-RESULTS-END -->"
if mutant_start not in readme or mutant_end not in readme:
    raise SystemExit("Mutation markers missing from README")
rows = [
    "| Мутант | Зориудын өөрчлөлт | Унасан тест | Failure/Error | Нотолгоо |",
    "|---|---|---|---:|---|",
]
labels = {
    1: "идэвхгүй оюутны guard-ыг авсан",
    2: "saveRegistration дуудалтыг авсан",
    3: "courseId null шалгалтыг авсан",
}
for n in (1, 2, 3):
    text = required(f"mvn-test-mutant-{n}.txt")
    line = summary(text)
    m = re.search(r"Tests run: (\d+), Failures: (\d+), Errors: (\d+), Skipped: (\d+)", line)
    if not m:
        raise SystemExit(f"Mutant {n}: summary parse failed")
    total, failures, errors, skipped = map(int, m.groups())
    if total != 10 or failures + errors < 1 or "BUILD FAILURE" not in text:
        raise SystemExit(f"Mutant {n} did not meet mutation criteria: {line}")
    names = sorted(name for name in set(re.findall(r"CourseRegistrationServiceTest\.([A-Za-z0-9_]+)", text)) if name != "java")
    failed_tests = ", ".join(tick + x + tick for x in names) if names else "Maven log-оос шалгана"
    rows.append(f"| M{n} | {labels[n]} | {failed_tests} | {failures} Failure, {errors} Error | " + tick + f"results/mvn-test-mutant-{n}.txt" + tick + " |")
mutation = "\n".join(rows) + "\n\n" + "\n".join(
    f"- M{n}: {tick}{summary(required(f'mvn-test-mutant-{n}.txt'))}{tick}" for n in (1, 2, 3)
) + "\n"
pre, tail = readme.split(mutant_start, 1)
_, post = tail.split(mutant_end, 1)
readme_path.write_text(pre + mutant_start + "\n\n" + mutation + "\n" + mutant_end + post, encoding="utf-8")
print("README.md-д бодит Java/Maven болон тестийн үр дүнг орууллаа.")
