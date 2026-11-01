#!/usr/bin/env bash
# Sets up one user, one habit, one tree, and one log against a fresh app.
BASE=${BASE:-localhost:8080}
H='Content-Type: application/json'

echo "--- create user"
curl -s -X POST "$BASE/users" -H "$H" \
  -d '{"name":"Pratham","email":"p@example.com","targetProfile":"STUDENT"}'; echo

echo "--- create habit"
curl -s -X POST "$BASE/users/1/habits" -H "$H" \
  -d '{"name":"Reading","type":"POSITIVE","weight":2,"dailyTarget":5}'; echo

echo "--- create tree"
curl -s -X POST "$BASE/users/1/tree" -H "$H" \
  -d '{"name":"Oak","species":"OAK"}'; echo

echo "--- log habit"
curl -s -X POST "$BASE/users/1/logs" -H "$H" \
  -d '{"habitId":1,"quantity":1}'; echo

echo "--- current tree"
curl -s "$BASE/users/1/tree"; echo

echo "--- history"
curl -s "$BASE/users/1/tree/history"; echo
