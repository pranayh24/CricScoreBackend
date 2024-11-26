from flask import Flask, request, jsonify
import pandas as pd
from werkzeug.serving import WSGIRequestHandler
import logging
import os
from datetime import datetime
import sys

from model.cricket_prediction_system import CricketPredictionSystem

# Configure logging
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(levelname)s - %(message)s',
    handlers=[
        logging.FileHandler('cricket_api.log'),
        logging.StreamHandler()
    ]
)
logger = logging.getLogger(__name__)

# Create Flask app
app = Flask(__name__)

# Global variable for the predictor
predictor = None


def initialize_model():
    """Initialize the prediction system with training data"""
    global predictor
    try:
        data_dir = os.path.join(r"D:\projects\CricPred", 'data')
        analysis_file = os.path.join(data_dir, 'ODI_Match_info.csv')
        match_details_file = os.path.join(data_dir, 'cricket_match_analysis.csv')
        model_file = os.path.join(os.path.dirname(os.path.abspath(__file__)), 'cricket_prediction_system.pkl')

        if os.path.exists(model_file):
            logger.info("Loading saved model...")
            predictor = CricketPredictionSystem.load_models(model_file)
            logger.info("Model loaded successfully")
        else:
            logger.info("Training new model...")
            if not os.path.exists(analysis_file) or not os.path.exists(match_details_file):
                raise FileNotFoundError(f"Data files not found in {data_dir}")

            predictor = CricketPredictionSystem()
            analysis_df = pd.read_csv(analysis_file, low_memory=False)
            match_details_df = pd.read_csv(match_details_file, low_memory=False)
            realtime_df, final_scores_df = predictor.prepare_enhanced_dataset(analysis_df, match_details_df)
            metrics = predictor.train_models(realtime_df, final_scores_df)
            logger.info(f"Model trained successfully. Metrics: {metrics}")
            predictor.save_models(model_file)

        return True
    except Exception as e:
        logger.error(f"Error initializing model: {str(e)}")
        return False


def determine_batting_team(match_info):
    """
    Determine the current batting team based on match information
    """
    is_second_innings = match_info.get('target') is not None
    team1 = match_info['team1']
    team2 = match_info['team2']
    toss_winner = match_info['toss_winner']
    toss_decision = match_info['toss_decision']

    # First determine who batted first
    if toss_winner == team1:
        first_batting_team = team1 if toss_decision == 'bat' else team2
    else:  # toss_winner == team2
        first_batting_team = team2 if toss_decision == 'bat' else team1

    # Then return the appropriate team based on the innings
    if is_second_innings:
        return team2 if first_batting_team == team1 else team1
    else:
        return first_batting_team


# Initialize the model at startup
logger.info("Initializing model at startup...")
if not initialize_model():
    logger.error("Failed to initialize model at startup")
    sys.exit(1)
logger.info("Model initialization completed")


@app.route('/health', methods=['GET'])
def health_check():
    """Endpoint to check if the service is up and the model is loaded"""
    return jsonify({
        'status': 'healthy',
        'model_loaded': predictor is not None,
        'timestamp': datetime.now().isoformat()
    })


@app.route('/predict', methods=['POST'])
def predict_match():
    """Unified endpoint for match predictions"""
    try:
        data = request.json
        logger.info(f"Received prediction request: {data}")

        # Validate required fields
        required_fields = ['team1', 'team2', 'city', 'current_score',
                           'current_wickets', 'current_over', 'toss_winner',
                           'toss_decision']

        # Check if target is provided to determine innings
        is_second_innings = 'target' in data
        if is_second_innings:
            required_fields.append('target')

        missing_fields = [field for field in required_fields if field not in data]
        if missing_fields:
            return jsonify({
                'error': 'Missing required fields',
                'missing_fields': missing_fields
            }), 400

        # Prepare match info
        match_info = {
            'team1': data['team1'],
            'team2': data['team2'],
            'city': data['city'],
            'current_score': float(data['current_score']),
            'current_wickets': int(data['current_wickets']),
            'current_over': float(data['current_over']),
            'toss_winner': data['toss_winner'],
            'toss_decision': data['toss_decision'],
            'batting_first': 0 if is_second_innings else 1,
            'target': float(data['target']) if is_second_innings else None
        }

        # Make prediction
        prediction = predictor.predict(match_info)
        logger.info(f"Prediction successful: {prediction}")

        # Determine current batting team
        batting_team = determine_batting_team(match_info)
        bowling_team = match_info['team2'] if batting_team == match_info['team1'] else match_info['team1']

        # Determine likely winner based on win probabilities
        likely_winner = match_info['team1'] if prediction.get('team1_win_probability', 0) > prediction.get(
            'team2_win_probability', 0) else match_info['team2']

        # Calculate predicted score based on innings and likely winner
        if not is_second_innings:
            predicted_score = prediction.get('predicted_final_score')
        else:
            if likely_winner != batting_team:
                predicted_score = prediction.get('predicted_final_score')
            else:
                predicted_score = match_info['target'] + 1

        # Format the response
        formatted_prediction = {
            'status': 'success',
            'innings': '2nd' if is_second_innings else '1st',
            'batting_team': batting_team,
            'bowling_team': bowling_team,
            'city': match_info['city'],
            'toss_winner': match_info['toss_winner'],
            'toss_decision': match_info['toss_decision'],
            'current_score': match_info['current_score'],
            'current_wickets': match_info['current_wickets'],
            'current_over': match_info['current_over'],
            'target': match_info['target'] if is_second_innings else None,
            'predicted_final_score': predicted_score,
            'win_probability': {
                match_info['team1']: prediction.get('team1_win_probability'),
                match_info['team2']: prediction.get('team2_win_probability')
            },
            'likely_winner': likely_winner,
            'timestamp': datetime.now().isoformat()
        }

        return jsonify(formatted_prediction)

    except Exception as e:
        logger.error(f"Error in prediction: {str(e)}")
        return jsonify({
            'error': 'Prediction failed',
            'message': str(e)
        }), 500

def main():
    """Main function to run the Flask app"""
    try:
        WSGIRequestHandler.protocol_version = "HTTP/1.1"
        app.run(host='0.0.0.0', port=5000, threaded=False)
    except Exception as e:
        logger.error(f"Error starting Flask app: {str(e)}")
        sys.exit(1)


if __name__ == '__main__':
    main()