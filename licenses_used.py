import requests
import base64
import json
import sys
import datetime
import time

# ==== CONFIGURATION ====
COLLAB_URL = "http://localhost:8010/services/json/v1"
LOGIN = "admin"
PASSWORD = "admin"
VERIFY_SSL = False

# ==== API CALLS ====
commands = [
    {"command": "Examples.checkLoggedIn"},
    {"command": "SessionService.getLoginTicket", "args": {"login": LOGIN, "password": PASSWORD}},
    {"command": "Examples.checkLoggedIn"},
    {"command": "AdminLicenseService.getLicenseUsageData"}
]


def get_license_usage_data():
    try:
    #    print(" Sending request to Collaborator...")
        response = requests.post(COLLAB_URL, json=commands, verify=VERIFY_SSL)
    except Exception as e:
        print(f" Failed to connect: {e}")
        sys.exit(1)

    if response.status_code != 200:
        print(f" HTTP error {response.status_code}:")
        print(response.text)
        sys.exit(1)

    try:
        data = response.json()
    except json.JSONDecodeError:
        print(" Response is not valid JSON.")
        print(response.text)
        sys.exit(1)

    print(datetime.datetime.now().strftime("%Y-%m-%d %H:%M:%S"))
    print(json.dumps(data[3].get("result", ()), indent=2))
    print("")

while True:
    get_license_usage_data()
    time.sleep(5*60)

sys.exit(0)

