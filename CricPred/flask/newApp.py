from flask import Flask, request, jsonify
from werkzeug.serving import WSGIRequestHandler
import logging
import os
from datetime import datetime
import sys

from model.cricket_prediction_system import CricketPredictionSystem
from model.cricket_prediction_system_t20 import CricketPredictionSystemT20  # Import T20 model

# Configure logging
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(levelname)s - %(message)s',
    handlers=[
        logging.FileHandler('../cricket_api.log'),
        logging.StreamHandler()
    ]
)
logger = logging.getLogger(__name__)

# Create Flask app
app = Flask(__name__)

# Global variables for the predictors
odi_predictor = None
t20_predictor = None

def initialize_model():
    """Initialize the prediction systems with training data"""
    global odi_predictor, t20_predictor
    try:
        data_dir = os.path.join(r"/", 'data')
        model_dir = os.path.dirname(os.path.abspath(__file__))

        # Load ODI model
        odi_model_file = os.path.join(model_dir, 'cricket_prediction_system.pkl')
        if os.path.exists(odi_model_file):
            logger.info("Loading saved ODI model...")
            odi_predictor = CricketPredictionSystem.load_models(odi_model_file)
            logger.info("ODI model loaded successfully")
        else:
            logger.error("ODI model file not found.")
            return False

        # Load T20 model
        t20_model_file = os.path.join(model_dir, 't20_cricket_predictor.pkl')
        if os.path.exists(t20_model_file):
            logger.info("Loading saved T20 model...")
            t20_predictor = CricketPredictionSystemT20.load_models(t20_model_file)
            logger.info("T20 model loaded successfully")
        else:
            logger.error("T20 model file not found.")
            return False

        return True
    except Exception as e:
        logger.error(f"Error initializing models: {str(e)}")
        return False

# Initialize the models at startup
logger.info("Initializing models at startup...")
if not initialize_model():
    logger.error("Failed to initialize models at startup")
    sys.exit(1)
logger.info("Model initialization completed")

@app.route('/health', methods=['GET'])
def health_check():
    """Endpoint to check if the service is up and the models are loaded"""
    return jsonify({
        'status': 'healthy',
        'models_loaded': {
            'ODI': odi_predictor is not None,
            'T20': t20_predictor is not None
        },
        'timestamp': datetime.now().isoformat()
    }), 200

@app.route('/predict', methods=['POST'])
def predict_match():
    """Unified endpoint for match predictions"""
    try:
        data = request.json
        logger.info(f"Received prediction request: {data}")

        # Validate required fields
        required_fields = ['team1', 'team2', 'city', 'current_score',
                           'current_wickets', 'current_over', 'toss_winner',
                           'toss_decision', 'batting_team']

        # Check if target is provided to determine innings
        is_second_innings = 'target' in data
        if is_second_innings:
            required_fields.append('target')

        # Check for match_format parameter
        match_format = data.get('match_format', 'ODI').upper()
        if match_format not in ['ODI', 'T20']:
            return jsonify({
                'error': 'Invalid match_format',
                'message': 'match_format must be either "ODI" or "T20"'
            }), 400

        # Check for missing fields
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
            'batting_first': int(data.get('batting_first', 1)),
            'target': float(data['target']) if is_second_innings else None,
            'batting_team': data['batting_team']
        }

        # Select the appropriate predictor
        if match_format == 'ODI':
            predictor_to_use = odi_predictor
        elif match_format == 'T20':
            predictor_to_use = t20_predictor
        else:
            return jsonify({
                'error': 'Invalid match_format',
                'message': 'match_format must be either "ODI" or "T20"'
            }), 400

        # Validate that the predictor is loaded
        if predictor_to_use is None:
            return jsonify({
                'error': f'{match_format} model not loaded',
                'message': f'The {match_format} model could not be loaded.'
            }), 500

        # Make prediction
        prediction = predictor_to_use.predict(
            team1=match_info['team1'],
            team2=match_info['team2'],
            city=match_info['city'],
            batting_team=match_info['batting_team'],
            current_score=match_info['current_score'],
            current_wickets=match_info['current_wickets'],
            current_over=match_info['current_over'],
            target=match_info['target'],
            toss_winner=match_info['toss_winner'],
            toss_decision=match_info['toss_decision'],
            batting_first=match_info['batting_first']
        )
        logger.info(f"Prediction successful: {prediction}")

        # Format the response
        response = {
            'status': 'success',
            'match_format': match_format,
            'batting_team': prediction['batting_team'],
            'bowling_team': prediction['bowling_team'],
            'city': prediction['city'],
            'current_over': prediction['current_over'],
            'current_score': prediction['current_score'],
            'current_wickets': prediction['current_wickets'],
            'innings': prediction['innings'],
            'likely_winner': prediction['likely_winner'],
            'predicted_final_score': prediction['predicted_final_score'],
            'target': prediction['target'],
            'timestamp': prediction['timestamp'],
            'toss_decision': prediction['toss_decision'],
            'toss_winner': prediction['toss_winner'],
            'win_probability': prediction['win_probability']
        }

        return jsonify(response)

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