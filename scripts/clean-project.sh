# Allow to call this script with the project already provided
projectLocation="$1"

SCRIPT="$(basename "${0}")"
if [ "${#}" -lt 1 ]; then
  echo "Script to compile and clean the given project (in a temporary directory) as preparation for source code analysis"
  echo "Usage: ./${SCRIPT} <project location>"
  echo "Requirements:"
  echo " - Project is a compilable Maven project."
  exit 1
fi

# Check whether the provided location exists
if [ ! -d "$projectLocation" ]; then
  echo "Project directory ${projectLocation} was not found"
  exit 1
fi

if [[ ${projectLocation} != */ ]]; then
  projectLocation="${projectLocation}/"
fi

tmpProject="$(mktemp -d)"
trap 'rm -rf -- "${tmpProject=}"' ERR EXIT HUP INT TERM
echo "Storing project in temporary location ${tmpProject}"

# Copy the project to the temporary project
cp -r "${projectLocation}." "${tmpProject}"

mvn -f "$tmpProject/pom.xml" clean compile -DskipTests

# Clean the jar if requested. It is better to include them to resolve more AST nodes and therefore potentially increase
# coverage, but it will slow down analysis as more classes need to be checked when resolving an AST node.
while true; do
  echo "Do you wish to clean the .jars as well? Doing so would drastically fasten the static analysis but would result in that less relationships can be resolved due to library usage where library jars are missing."
  read -r -p "Do you wish to clean .jars? [Yn]" yn
  case $yn in
      [Yy]* ) find "$tmpProject" -type f -name '*.jar' -print0 | xargs -0 rm; break;;
      [Nn]* ) break;;
      * ) find "$tmpProject" -type f -name '*.jar' -print0 | xargs -0 rm; break;;
  esac
done

# Remove test related directories -- these are not parsed and will slow down analysis a lot
find "$tmpProject" -type d -name test -print0 | xargs -0 rm -rf
find "$tmpProject" -type d -name test-classes -print0 | xargs -0 rm -rf
find "$tmpProject" -type d -name generated-test-sources -print0 | xargs -0 rm -rf

# Remove git logs -- primarily for security as we don't need it
find "$tmpProject" -type d -name '.git' -print0 | xargs -0 rm -rf

# Zip the cleaned project and place it in the designated location
targetLocation=$(realpath ../src/test/resources/project-cleaned.zip)
cd "$tmpProject" && zip -r "$targetLocation" ./*
echo "Stored resulting ZIP in ${targetLocation}"

read -n 1 -r -p "Stored resulting ZIP in ${targetLocation}. Press any key to wipe the temporary directory."
