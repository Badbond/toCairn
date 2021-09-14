# Allow to call this script with the project already provided
projectLocation="$1"
targetLocation="$2"

SCRIPT="$(basename "${0}")"
if [ "${#}" -lt 2 ]; then
  echo "Script to compile and clean the given project (in a temporary directory) as preparation for source code analysis"
  echo "Usage: ./${SCRIPT} <project location> <target ZIP to create>"
  echo "Requirements:"
  echo " - Project is a compilable Maven project."
  exit 1
fi

# Transform paths into absolute paths
projectLocation=$(realpath "${projectLocation}")
targetLocation="$(realpath "${targetLocation}")"

# Check whether the provided location exists
if [ ! -d "$projectLocation" ]; then
  echo "Project directory ${projectLocation} was not found"
  exit 1
fi
if [ ! -d "$(dirname $targetLocation)" ]; then
  echo "Parent directory for target ${targetLocation} was not found"
  exit 1
fi

# Add trailing slash if not given
[[ "${projectLocation}" != */ ]] && projectLocation="${projectLocation}/"

tmpProject="$(mktemp -d)"
trap 'rm -rf -- "${tmpProject=}"' EXIT HUP INT TERM
echo "Storing project in temporary location ${tmpProject}"

# Copy the project to the temporary project
cp -r "${projectLocation}." "${tmpProject}"

echo "Compiling project"
mvn -T 1.0C -f "$tmpProject/pom.xml" clean install -DskipTests >/dev/null

# Clean the jar if requested. It is better to include them to resolve more AST nodes and therefore potentially increase
# coverage, but it will slow down analysis as more classes need to be checked when resolving an AST node.
while true; do
  echo "Do you wish to clean the .jars as well?"
  echo "Doing so would drastically fasten the static analysis but would result in that less relationships can be resolved due to library usage where library jars are missing."
  echo "Next, we will ask for a glob for which will keep .jar files with."
  echo "If you fill in '*', we will keep all .jar files. If you fill in '', we will remove all .jar files"
  echo "If you fill in \"*exec.jar\" we will remove all .jar files except those ending with exec.jar."
  read -r -p "With what glob do you wish to keep .jar files?" glob
  case $glob in
      [''] ) find "$tmpProject" -type f -name '*.jar' -print0 | xargs -0 rm; break;;
      * ) find "$tmpProject" -type f -name '*.jar' -not -name "$glob" -print0 | xargs -0 rm; break;;
  esac
done

# Remove test related directories -- these are not parsed and will slow down analysis a lot
find "$tmpProject" -type d -name test -print0 | xargs -0 rm -rf
find "$tmpProject" -type d -name test-classes -print0 | xargs -0 rm -rf
find "$tmpProject" -type d -name generated-test-sources -print0 | xargs -0 rm -rf

# Remove git logs -- primarily for security as we don't need it
find "$tmpProject" -type d -name '.git' -print0 | xargs -0 rm -rf

# Zip the cleaned project and place it in the designated location
cd "$tmpProject" && zip -r "$targetLocation" ./*

read -n 1 -r -p "Stored resulting ZIP in ${targetLocation}. Press any key to wipe the temporary directory."
