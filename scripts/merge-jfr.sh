# Based on https://stackoverflow.com/a/60976616

jfrDirectory="$1"
targetFile="$2"

SCRIPT="$(basename "${0}")"
if [ "${#}" -lt 2 ]; then
  echo "Script to merge JFR files into one. Performs recursive lookup in given directory for .jfr files."
  echo "Then merges them into a single one, one by one, optionally deleting the source to save space."
  echo "Usage: ./${SCRIPT} <JFR directory> <target .jfr file>"
  exit 1
fi

# Transform paths into absolute paths
jfrDirectory=$(realpath "${jfrDirectory}")
targetFile="$(realpath "${targetFile}")"

# Check whether the provided location exists
if [ ! -d "$jfrDirectory" ]; then
  echo "JFR directory ${jfrDirectory} was not found"
  exit 1
fi

echo "Essentially, we will be duplicating space on the HDD otherwise."
while true; do
  read -r -p "To save space, do you wish to remove the old .jfr files? [Yn]" answer
  case $answer in
      y|Y ) deleteFile=true; break;;
      n|N ) deleteFile=false; break;;
      [''] ) deleteFile=true; break;;
      * ) echo "invalid answer $answer"
  esac
done


if [ ! -f "$targetFile" ]; then
  echo "$targetFile does not exist, creating it."
  touch "$targetFile"
fi

find "$jfrDirectory" -name '*.jfr' \
  | while read JFR_FILE; do
    cat "$JFR_FILE" >> "$targetFile"
    echo "Pushing $JFR_FILE to $targetFile"
    if [ "$deleteFile" ]; then
      echo "Deleted $JFR_FILE"
      rm "$JFR_FILE"
    fi
  done
