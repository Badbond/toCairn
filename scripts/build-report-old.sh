times=${1:-1}
echo "Running script $times times"

echo "Running install script without tests to install everything in local artifactory"
echo "This would prevent the first execution to be longer than the others"
mvn -T 1.0C clean install -DskipTests > /dev/null

echo "Setting up dependencies"
docker-compose --log-level ERROR down
sleep 5
docker-compose --log-level ERROR up -d mongodb rabbitmq

echo "Running build(s)"
total_start=`date +%s.%N`

for ((i=1; i<=times; i++))
do
    start=$(date +%s.%N)
    mvn -e -T 1.0C clean jacoco:prepare-agent install jacoco:report -Pintegration-tests > /dev/null
    status=$?
    end=$(date +%s.%N)
    runtime=$( echo "$end - $start" | bc -l)
    pretty_runtime=$(date -d@$runtime -u +%M:%S:%N)
    echo "Run $i took $pretty_runtime (M:S:ns) and gave exit code $status"
done

total_end=`date +%s.%N`
total_runtime=$( echo "$total_end - $total_start" | bc -l)
total_pretty_runtime=$(date -d@$total_runtime -u +%H:%M:%S:%N)
echo "Total runtime of $times builds took $total_pretty_runtime (H:M:S:ns)"
average=$( echo "$total_runtime / $times" | bc -l)
echo "That is an average of $(date -d@$average -u +%M:%S:%N) (M:S:ns)"

echo "Combining coverage and generating report of last build"
output=$(find . -name jacoco.exec -print0 | xargs -0)
java -jar ../../Badbond/jacoco/jacoco/target/lib/jacococli.jar merge \
  --destfile target/jacoco.exec $output > /dev/null
java -jar ../../Badbond/jacoco/jacoco/target/lib/jacococli.jar report target/jacoco.exec \
  --html target/report --xml target/jacoco.xml $(find "${@:3}" -path "*/target/classes" | sed 's/^/--classfiles /') \
  $(find "${@:3}" -path "*/src/main/java" | sed 's/^/--sourcefiles /') \
  $(find "${@:3}" -path "*/target/generated-sources/annotations" | sed 's/^/--sourcefiles /') > /dev/null
echo "Report can be found at ./target/report/index.html and ./target/jacoco.xml"

read -p "Do you want to open the HTML report in a browser? [y/N] " -n 1 -r
if [[ $REPLY =~ ^[Yy]$ ]]
then
  xdg-open target/report/index.html
fi

echo "Done"
