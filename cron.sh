#!/bin/bash

# cronab -e
# run every 15 minutes, every hour, Mon-Fri
# */15 * * * 1-5 cd /Users/you/Code/financial-quant && ./cron.sh >> log/cron.log 2>&1

currenttime=$(date +%H:%M)
day=$(date +%Y-%m-%d)

# marketly closes at 1400 so wait a bit for data to catch up
if [[ "$currenttime" > "14:28" ]] ; then
  if [ -d "cache/$day" ];
  then
    echo "Cache for $day already exists." > /dev/null
  else
    echo "Time to pull data for $day"
    lein run -m financial-quant.cron/fetch-all
  fi
else
  echo "Not time yet" > /dev/null
fi


