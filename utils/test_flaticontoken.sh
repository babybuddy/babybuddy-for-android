#!/bin/bash

if [[ ! -e "$1" ]] ; then
  exit 2
fi

data=$(curl $CURL_ARGS -s -X GET -H "Accept: application/json" -H "Authorization: Bearer $( cat $1 )" https://api.flaticon.com/v2/total/icons | jq -r .data.total | sed -E 's/^[0-9]+$/true/')
exit $( [ x"$data" == xtrue ] ; echo $? )

