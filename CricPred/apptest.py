import requests


# Testing ODI Scenario 1
odi_payload = {
    "team1": "India",
    "team2": "Australia",
    "city": "Mumbai",
    "batting_team": "India",
    "current_score": 150,
    "current_wickets": 2,
    "current_over": 30.0,
    "toss_winner": "India",
    "toss_decision": "bat",
    "match_format": "ODI"
}

response_odi = requests.post("http://localhost:5000/predict", json=odi_payload)
print("ODI Scenario 1 Prediction:")
print(response_odi.json())

# Testing T20 Scenario 1
t20_payload = {
    "team1": "Pakistan",
    "team2": "New Zealand",
    "city": "Dubai",
    "batting_team": "Pakistan",
    "current_score": 60,
    "current_wickets": 1,
    "current_over": 6.0,
    "toss_winner": "New Zealand",
    "toss_decision": "field",
    "match_format": "T20"
}

response_t20 = requests.post("http://localhost:5000/predict", json=t20_payload)
print("\nT20 Scenario 1 Prediction:")
print(response_t20.json())