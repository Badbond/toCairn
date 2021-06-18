# Allow to call this script with the project already provided
projectLocation="$1"

# If it was not provided, ask for it
if [ -z "${projectLocation}" ]; then
  echo "Where is the project located?"
  read -r projectLocation
fi

echo "We are about to process project ${projectLocation}. Please make sure that this project is compiled such that
 generated classes are present in the project directory as well. We will copy the project to a temporary directory,
 and within this temporary directory remove (generated) test classes, remove .git directories, and optionally remove
 .jars. Then we will zip the project and store it in a git ignored test location within this application."

# Check whether the provided location exists
if [ ! -d "$projectLocation" ]; then
  echo "Project directory ${projectLocation} was not found"
  exit 1
fi

tmpProject="/tmp/$(uuidgen)"

# Remove existing temporary project
if [ ! -d tmpProject ]; then
  rm -rf tmpProject
fi

# Copy the project to the temporary project
mkdir "$tmpProject"
cp -r "$projectLocation" "$tmpProject"

# Clean the jar if requested. It is better to include them to resolve more AST nodes and therefore potentially increase
# coverage, but it will slow down analysis as more classes need to be checked when resolving an AST node.
while true; do
  read -r -p "Do you wish to clean .jars as well? [Yn]" yn
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
targetLocation=$(realpath ./src/test/resources/big-project-cleaned.zip)
rm "$targetLocation"
cd "$tmpProject" && zip -r "$targetLocation" ./*

# Remove the temporary project
rm -rf "$tmpProject"
