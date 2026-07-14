#!/usr/bin/env bash

GREEN="\033[0;32m"
RED="\033[0;31m"
NC="\033[0m"

check_site() {
    local name="$1"
    local url="$2"
    local expected="$3"

    status=$(curl -L -s -o /dev/null -w "%{http_code}" "$url")

    if [[ "$status" == "$expected" ]]; then
        printf "${GREEN}✓ PASS${NC} %-30s %s (HTTP %s)\n" "$name" "$url" "$status"
    else
        printf "${RED}✗ FAIL${NC} %-30s %s (Expected %s, Got %s)\n" \
            "$name" "$url" "$expected" "$status"
    fi
}

echo "========================================="
echo " Website Health Check"
echo " $(date)"
echo "========================================="

check_site "Automation Testing"      "https://automationintesting.online"              "200"
check_site "Restful Booker Ping"     "https://restful-booker.herokuapp.com/ping"       "201"
check_site "PST (With Bugs)"         "https://with-bugs.practicesoftwaretesting.com"   "200"
check_site "Practice Software"       "https://practicesoftwaretesting.com"             "200"
check_site "Automation Exercise"     "https://automationexercise.com"                  "200"
check_site "OrangeHRM Demo"          "https://opensource-demo.orangehrmlive.com"       "200"

echo "========================================="