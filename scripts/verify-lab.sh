#!/usr/bin/env bash
set -Eeuo pipefail
ROOT_DIR="$(cd "$(dirname "\${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"
mkdir -p results
command -v java >/dev/null 2>&1 || { echo "Java олдсонгүй" >&2; exit 127; }
command -v mvn >/dev/null 2>&1 || { echo "Maven олдсонгүй" >&2; exit 127; }
java -version 2>&1 | tee results/java-version.txt
mvn -version 2>&1 | tee results/mvn-version.txt
SERVICE="src/main/java/mn/edu/must/sqat/CourseRegistrationService.java"
BACKUP="$(mktemp)"
cp "$SERVICE" "$BACKUP"
restore_source() { cp "$BACKUP" "$SERVICE"; }
cleanup() { restore_source; rm -f "$BACKUP"; }
trap cleanup EXIT
run_expected_failure() {
  local number="$1" description="$2"
  local output="results/mvn-test-mutant-\${number}.txt"
  local status
  echo
  echo "========== MUTANT \${number}: \${description} =========="
  set +e
  mvn test 2>&1 | tee "$output"
  status=\${PIPESTATUS[0]}
  set -e
  if [[ "$status" -eq 0 ]]; then
    echo "Мутант \${number}-ийг тест илрүүлсэнгүй." >&2
    return 1
  fi
  grep -Eq 'Failures: [1-9][0-9]*|Errors: [1-9][0-9]*' "$output" || {
    echo "Mutant \${number}: Failure/Error байхгүй." >&2; return 1;
  }
  grep -q 'BUILD FAILURE' "$output" || {
    echo "Mutant \${number}: BUILD FAILURE олдсонгүй." >&2; return 1;
  }
  restore_source
}
python3 - "$SERVICE" <<'PY'
from pathlib import Path
import sys
p = Path(sys.argv[1])
s = p.read_text(encoding="utf-8")
old = '        if (!student.active()) return "ERROR_INACTIVE_STUDENT";'
new = '        // MUTANT 1: inactive-student guard removed.'
assert s.count(old) == 1
p.write_text(s.replace(old, new), encoding="utf-8")
PY
run_expected_failure 1 "inactive-student guard removed"
python3 - "$SERVICE" <<'PY'
from pathlib import Path
import sys
p = Path(sys.argv[1])
s = p.read_text(encoding="utf-8")
old = '        repository.saveRegistration(studentId, courseId);'
new = '        // MUTANT 2: registration persistence removed.'
assert s.count(old) == 1
p.write_text(s.replace(old, new), encoding="utf-8")
PY
run_expected_failure 2 "saveRegistration call removed"
python3 - "$SERVICE" <<'PY'
from pathlib import Path
import sys
p = Path(sys.argv[1])
s = p.read_text(encoding="utf-8")
old = '        if (studentId == null || courseId == null) {'
new = '        if (studentId == null) {'
assert s.count(old) == 1
p.write_text(s.replace(old, new), encoding="utf-8")
PY
run_expected_failure 3 "courseId null validation removed"
restore_source
mvn test 2>&1 | tee results/mvn-test.txt
grep -Eq 'Tests run: 10, Failures: 0, Errors: 0, Skipped: 0' results/mvn-test.txt
grep -q 'BUILD SUCCESS' results/mvn-test.txt
python3 scripts/finalize-readme.py
echo "A1-A8, M1-M3 болон эцсийн 10 тест шалгагдлаа."
