#!/bin/bash

make -f help.makefile
if [[ $? -ne 0 ]] ; then
  exit 1
fi

set -e
for xml in strings.xml help_strings.xml; do
  echo "Checking $xml .."
  find app/src/main/res/ -name "$xml" | grep -E "/values-[a-z]*/" | while read txml ; do
    set +e
    echo ".. against '${txml##*/res/}'"
    python3 tools/verify-translation-file.py "app/src/main/res/values/$xml" "${txml}" > output.txt
    errorcode=$?
    cat output.txt | while read line ; do
      echo "       $line"
    done
    if [[ $errorcode -ne 0 ]] ; then
      exit 1
    fi
  done
done

