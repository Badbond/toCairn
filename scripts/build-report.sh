# This script is derived from the one which has been used to do performance benchmarking
# of various JaCoCo implementations. The script has been altered since those tests to use
# user provided directories and jar locations and to copy all required and generated resources
# to a temporary directory which is cleared after final prompt.

SCRIPT="$(basename "${0}")"
if [ "${#}" -lt 3 ]; then
  ehco "Script to do performance testing of a custom JaCoCo version on the given project."
  echo "Usage: ./${SCRIPT} <builds to run> <project location> <JaCoCo CLI .jar location>"
  echo "Requirements:"
  echo " - Project is a Maven project with JaCoCo maven plugin configured to the custom version."
  echo " - Docker-compose file in root of project directory to boot up dependencies (atm only services named 'rabbitmq' and 'mongodb' are booted)"
  echo " - The configured JaCoCo maven plugin has the same version as the cli .jar."
  exit 1
fi

TIMES="${1}"
PROJECT_LOCATION="${2}"
CLI_LOCATION="${3}"

if [ "${TIMES}" -le 0 ]; then
  echo "Must run at least once"
  exit 1
fi

if [ ! -d "${PROJECT_LOCATION}" ]; then
  echo "Could not find given project location: ${PROJECT_LOCATION}"
  exit 1
fi

if [[ ${PROJECT_LOCATION} != */ ]]; then
  PROJECT_LOCATION="${PROJECT_LOCATION}/"
fi

if [ ! -f "${CLI_LOCATION}" ]; then
  echo "Could not find given project location: ${CLI_LOCATION}"
  exit 1
fi

# Create temporary directory for this performance test
TMP_DIR="$(mktemp -d)"
trap 'rm -rf -- "${TMP_DIR=}"' ERR EXIT HUP INT TERM
echo "Copying resources to temporary directory ${TMP_DIR}"
cd "${TMP_DIR}" || exit 1
cp -r "${PROJECT_LOCATION}." "${TMP_DIR}"
cp "${CLI_LOCATION}" "${TMP_DIR}/jacococli.jar"

echo "Running install script without tests to install everything in local artifactory"
echo "This would prevent the first execution to be longer than the others"
mvn -T 1.0C clean install -DskipTests >/dev/null

echo "Setting up dependencies"
# TODO: Parameterize services to boot
docker-compose --log-level ERROR -f docker-compose.yml up -d mongodb rabbitmq
trap 'docker-compose --log-level ERROR -f docker-compose.yml down && rm -rf -- "${TMP_DIR=}"' ERR EXIT HUP INT TERM

echo "Running build(s) ${TIMES} times"
TOTAL_START=$(date +%s.%N)

for ((i = 1; i <= TIMES; i++)); do
  TOTAL_START=$(date +%s.%N)
  # TODO: Parameterize profiles to run with
  mvn -e -T 1.0C clean jacoco:prepare-agent install jacoco:report -Pintegration-tests >/dev/null
  STATUS=$?
  END=$(date +%s.%N)
  RUNTIME=$(echo "${END} - ${TOTAL_START}" | bc -l)
  PRETTY_RUNTIME=$(date -d@"$RUNTIME" -u +%M:%S:%N)
  echo "Run ${i} took ${PRETTY_RUNTIME} (M:S:ns) and gave exit code ${STATUS}"
done

TOTAL_END=$(date +%s.%N)
TOTAL_RUNTIME=$(echo "$TOTAL_END - $TOTAL_START" | bc -l)
TOTAL_PRETTY_RUNTIME=$(date -d@"$TOTAL_RUNTIME" -u +%H:%M:%S:%N)
echo "Total RUNTIME of ${TIMES} builds took ${TOTAL_PRETTY_RUNTIME} (H:M:S:ns)"
AVERAGE=$(echo "$TOTAL_RUNTIME / $TIMES" | bc -l)
echo "That is an AVERAGE of $(date -d@"$AVERAGE" -u +%M:%S:%N) (M:S:ns)"

echo "Combining coverage and generating report of last build"
output=$(find . -name jacoco.exec -print0 | xargs -0)
java -jar jacococli.jar merge \
  --destfile jacoco.exec "$output" >/dev/null
java -jar jacococli.jar report jacoco.exec \
  --html html-report --xml jacoco.xml \
  $(find . -path "*/target/classes" | sed 's/^/--classfiles /') \
  $(find . -path "*/src/main/java" | sed 's/^/--sourcefiles /') \
  $(find . -path "*/target/generated-sources/annotations" | sed 's/^/--sourcefiles /') >/dev/null

read -p "Do you want to open the HTML report in a browser? [y/N] " -n 1 -r
if [[ $REPLY =~ ^[Yy]$ ]]; then
  if command -v xdg-open >/dev/null; then
    xdg-open html-report/index.html
  else
    open html-report/index.html
  fi
fi

read -n 1 -r -p "Last build reports can be found at ${TMP_DIR}/html-report/index.html and ${TMP_DIR}/jacoco.xml. Press any key to wipe these recording and reports."
