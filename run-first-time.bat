@ECHO OFF
start java -jar delorean-time-tracker-1.3.0.jar
ECHO Waiting for server start...
timeout 30
start http://localhost:9889