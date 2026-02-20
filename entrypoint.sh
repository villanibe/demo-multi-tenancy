#!/bin/bash
set -e

# Start Java in the background
# We don't use 'exec' here yet because we need the script to keep running for the warmup
/opt/java/bin/java $JAVA_TOOL_OPTIONS -jar /app/app.jar &
PID=$!

echo "Checking health at http://localhost:8080/actuator/health..."

# Wait for the app to be ready (Max 60 seconds)
MAX_RETRIES=30
COUNT=0
while ! curl -s http://localhost:8080/actuator/health | grep -q "UP"; do
  if [ $COUNT -eq $MAX_RETRIES ]; then
    echo "App failed to start in time. Exiting."
    kill $PID
    exit 1
  fi
  sleep 2
  COUNT=$((COUNT+1))
done

echo "App is UP. Running JIT warmup (500 requests)..."
for i in {1..500}; do
  curl -s http://localhost:8080/actuator/health > /dev/null
done

echo "Warmup complete. App is optimized and ready for traffic."

# Wait for the Java process so the container doesn't exit
wait $PID
