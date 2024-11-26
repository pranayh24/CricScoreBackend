import pandas as pd
from model.cricket_prediction_system import CricketPredictionSystem
# Initialize the prediction system
predictor = CricketPredictionSystem()

analysis_df = pd.read_csv(r"D:\projects\CricPred\data\cricket_match_analysis.csv",low_memory=False)
match_details_df = pd.read_csv(r"D:\projects\CricPred\data\ODI_Match_info.csv",low_memory=False)

# Train the models
print("Preparing dataset and training models...")
realtime_df, final_scores_df = predictor.prepare_enhanced_dataset(analysis_df, match_details_df)
metrics = predictor.train_models(realtime_df, final_scores_df)
print(f"Model Performance Metrics:")
print(f"Win Prediction Accuracy: {metrics['win_accuracy']:.3f}")
print(f"Score Prediction R2: {metrics['score_r2']:.3f}")
print(f"Score Prediction RMSE: {metrics['score_rmse']:.1f} runs")

# Example 1: First Innings Scenario (India batting first against Australia)
first_innings_info = {
    'team1': 'India',
    'team2': 'Australia',
    'city': 'Mumbai',
    'batting_team': 'India',
    'current_score': 167,
    'current_wickets': 3,
    'current_over': 30,
    'target': None,  # No target in first innings
    'toss_winner': 'India',
    'toss_decision': 'bat',
    'batting_first': 1  # Added this field
}

print("\nScenario 1: First Innings")
print("-------------------------")
print(f"Match: {first_innings_info['team1']} vs {first_innings_info['team2']} at {first_innings_info['city']}")
print(f"Current Score: {first_innings_info['current_score']}/{first_innings_info['current_wickets']}")
print(f"Overs: {first_innings_info['current_over']}.0")

first_innings_prediction = predictor.predict(first_innings_info)
print("\nPredictions:")
print(f"Predicted Final Score: {first_innings_prediction['predicted_final_score']}")
print(f"Win Probabilities:")
print(f"- {first_innings_info['team1']}: {first_innings_prediction['team1_win_probability']:.1%}")
print(f"- {first_innings_info['team2']}: {first_innings_prediction['team2_win_probability']:.1%}")
print(f"Prediction Confidence:")
print(f"- Win Prediction: {first_innings_prediction['prediction_confidence']['win_confidence']:.1%}")
print(f"- Score Prediction: {first_innings_prediction['prediction_confidence']['score_confidence']:.1%}")

# Example 2: Second Innings Scenario (Australia chasing)
second_innings_info = {
    'team1': 'India',
    'team2': 'Australia',
    'city': 'Mumbai',
    'batting_team': 'Australia',
    'current_score': 158,
    'current_wickets': 4,
    'current_over': 35,
    'target': 287,  # Target set by India
    'toss_winner': 'India',
    'toss_decision': 'bat',
    'batting_first': 0  # Added this field
}

print("\nScenario 2: Second Innings")
print("--------------------------")
print(f"Match: {second_innings_info['team1']} vs {second_innings_info['team2']} at {second_innings_info['city']}")
print(f"Target: {second_innings_info['target']}")
print(f"Current Score: {second_innings_info['current_score']}/{second_innings_info['current_wickets']}")
print(f"Overs: {second_innings_info['current_over']}.0")
print(f"Required Rate: {((second_innings_info['target'] - second_innings_info['current_score']) / ((50 - second_innings_info['current_over']) * 6) * 6):.2f}")

second_innings_prediction = predictor.predict(second_innings_info)
print("\nPredictions:")
print(f"Predicted Final Score: {second_innings_prediction['predicted_final_score']}")
print(f"Win Probabilities:")
print(f"- {second_innings_info['team1']}: {second_innings_prediction['team1_win_probability']:.1%}")
print(f"- {second_innings_info['team2']}: {second_innings_prediction['team2_win_probability']:.1%}")
print(f"Prediction Confidence:")
print(f"- Win Prediction: {second_innings_prediction['prediction_confidence']['win_confidence']:.1%}")
print(f"- Score Prediction: {second_innings_prediction['prediction_confidence']['score_confidence']:.1%}")

# Example 3: Critical Chase Scenario (Last 10 overs)
critical_chase_info = {
    'team1': 'India',
    'team2': 'Australia',
    'city': 'Mumbai',
    'batting_team': 'Australia',
    'current_score': 234,
    'current_wickets': 6,
    'current_over': 40,
    'target': 295,  # Challenging target
    'toss_winner': 'India',
    'toss_decision': 'bat',
    'batting_first': 0
}

print("\nScenario 3: Critical Chase")
print("-------------------------")
print(f"Match: {critical_chase_info['team1']} vs {critical_chase_info['team2']} at {critical_chase_info['city']}")
print(f"Target: {critical_chase_info['target']}")
print(f"Current Score: {critical_chase_info['current_score']}/{critical_chase_info['current_wickets']}")
print(f"Overs: {critical_chase_info['current_over']}.0")
print(f"Required Rate: {((critical_chase_info['target'] - critical_chase_info['current_score']) / ((50 - critical_chase_info['current_over']) * 6) * 6):.2f}")

critical_chase_prediction = predictor.predict(critical_chase_info)
print("\nPredictions:")
print(f"Predicted Final Score: {critical_chase_prediction['predicted_final_score']}")
print(f"Win Probabilities:")
print(f"- {critical_chase_info['team1']}: {critical_chase_prediction['team1_win_probability']:.1%}")
print(f"- {critical_chase_info['team2']}: {critical_chase_prediction['team2_win_probability']:.1%}")
print(f"Prediction Confidence:")
print(f"- Win Prediction: {critical_chase_prediction['prediction_confidence']['win_confidence']:.1%}")
print(f"- Score Prediction: {critical_chase_prediction['prediction_confidence']['score_confidence']:.1%}")

# Save the trained model for future use
predictor.save_models('cricket_prediction_system.pkl')