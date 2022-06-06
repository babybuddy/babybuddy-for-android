from email import header
import sys
import json
import requests
import re


print("=== Intial request ===")
url = sys.argv[1]
initial_response = requests.get(url)

print(json.dumps(dict(initial_response.headers), indent=4))

csrftoken = re.search(
    r"csrftoken=([^;]+);", initial_response.headers["set-cookie"]
).groups(1)[0]

middlewaretoken_input = re.search(
    r'''<input .*name=["']csrfmiddlewaretoken["'][^>]*>''',
    initial_response.text
).group(0)
print("middlewaretoken_input =", middlewaretoken_input)

middlewaretoken = re.split('''["']''', middlewaretoken_input.split("value=")[1])[1];
print("middlewaretoken =", middlewaretoken)

print("=== Login request ===")

req = requests.Request(
    "POST",
    url.rstrip("/") + "/login/",
    data=dict(
        csrfmiddlewaretoken=middlewaretoken,
        username="admin",
        password="admin",
        next="/",
    ),
    headers={
        "Referer": initial_response.url,
        "Cookie": f"csrftoken={csrftoken}",
    }
)
prepped_req = req.prepare()
print("prepped_req", prepped_req.headers, prepped_req.body)

s = requests.Session()
login_request = s.send(prepped_req)
print("code =", login_request.status_code)
