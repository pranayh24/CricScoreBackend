from datetime import datetime
import pandas as pd
import numpy as np
from sklearn.model_selection import train_test_split
from sklearn.preprocessing import StandardScaler, LabelEncoder
from sklearn.ensemble import RandomForestClassifier, RandomForestRegressor
from sklearn.metrics import accuracy_score, mean_squared_error, r2_score
import pickle


class CricketPredictionSystemT20:
    def __init__(self):
        self.win_model = None
        self.score_model = None
        self.win_scaler = None
        self.score_scaler = None
        self.features = None
        self.encoders = None
        self.team_rankings = {
            'India': 0.88,
            'England': 0.86,
            'Australia': 0.84,
            'Pakistan': 0.80,
            'South Africa': 0.78,
            'New Zealand': 0.76,
            'West Indies': 0.74,
            'Sri Lanka': 0.70,
            'Afghanistan': 0.68,
            'Bangladesh': 0.66,
            # Add other T20 teams as needed
        }

    def prepare_dataset(self, analysis_df):
        """Create dataset with enhanced features for T20 cricket."""
        realtime_data = []
        final_scores_data = []

        # Calculate venue statistics
        venue_stats = self._calculate_venue_stats(analysis_df)

        for idx, row in analysis_df.iterrows():
            team1_total = row['Team_1_Total_Runs']
            team1_wickets = row['Team_1_Total_Wickets']
            team2_total = row['Team_2_Total_Runs']
            team2_wickets = row['Team_2_Total_Wickets']

            # Run rate calculations for T20
            team1_rr = team1_total / 20
            team2_rr = team2_total / 20
            venue_avg = venue_stats.get(row['City'], {'avg_score': 150})['avg_score']

            for over in range(1, 21):
                # First innings data
                if row['Toss_Decision'] == 'bat':
                    batting_first_team = row['Toss_Winner']
                else:
                    batting_first_team = row['Team_2'] if row['Toss_Winner'] == row['Team_1'] else row['Team_1']

                if (row['Team_1'] == batting_first_team):
                    batting_first_score = int(team1_total * (over / 20))
                    batting_first_wickets = int(min(team1_wickets * (over / 20), 10))
                    current_run_rate = batting_first_score / over if over > 0 else 0
                    projected_score = self._project_score(batting_first_score, over, batting_first_wickets)

                    match_state = self._create_match_state(
                        row, over, batting_first_score, batting_first_wickets,
                        current_run_rate, None, None, 1, team1_total,
                        venue_avg, projected_score
                    )
                    realtime_data.append(match_state)
                    final_scores_data.append({**match_state, 'final_score': team1_total})

                # Second innings data
                else:
                    if over > 1:
                        batting_second_score = int(team2_total * ((over - 1) / 19))
                        batting_second_wickets = int(min(team2_wickets * ((over - 1) / 19), 10))
                        target = team1_total + 1
                        balls_remaining = (20 - over) * 6
                        required_runs = target - batting_second_score
                        required_rr = (required_runs / balls_remaining) * 6 if balls_remaining > 0 else 99.99
                        current_run_rate = batting_second_score / over if over > 0 else 0
                        chase_pressure = self._calculate_chase_pressure(required_rr, current_run_rate,
                                                                        batting_second_wickets)

                        match_state = self._create_match_state(
                            row, over, batting_second_score, batting_second_wickets,
                            current_run_rate, required_rr, target, 0, team2_total,
                            venue_avg, projected_score, chase_pressure
                        )
                        realtime_data.append(match_state)
                        final_scores_data.append({**match_state, 'final_score': team2_total})

        return pd.DataFrame(realtime_data), pd.DataFrame(final_scores_data)

    def _calculate_venue_stats(self, df):
        """Calculate venue statistics using analysis_df data."""
        venue_stats = {}
        for city in df['City'].unique():
            city_matches = df[df['City'] == city]
            if len(city_matches) > 0:
                venue_stats[city] = {
                    'avg_score': (city_matches['Team_1_Total_Runs'].mean() +
                                  city_matches['Team_2_Total_Runs'].mean()) / 2,
                    'chase_success_rate': len(city_matches[
                                                  city_matches['Winner'] == city_matches['Team_2']
                                                  ]) / len(city_matches) if len(city_matches) > 0 else 0
                }
            else:
                venue_stats[city] = {
                    'avg_score': 0,
                    'chase_success_rate': 0
                }
        return venue_stats

    def _project_score(self, current_score, current_over, wickets):
        """Project final score based on current situation in T20 cricket."""
        if current_over == 0:
            return 150  # Average T20 score

        remaining_overs = 20 - current_over
        current_run_rate = current_score / current_over

        # Adjust projection based on wickets in hand
        wickets_factor = max(0.5, (10 - wickets) / 10)

        # Progressive acceleration factor for T20
        if current_over < 10:
            acceleration = 1.1
        elif current_over < 15:
            acceleration = 1.2
        else:
            acceleration = 1.3

        projected_remaining = remaining_overs * current_run_rate * wickets_factor * acceleration
        return int(current_score + projected_remaining)

    def _calculate_chase_pressure(self, required_rr, current_rr, wickets):
        """Calculate chase pressure index."""
        rr_pressure = max(0, (required_rr - current_rr) / 2)
        wickets_pressure = wickets / 10
        return (rr_pressure + wickets_pressure) / 2

    def _create_match_state(self, row, over, score, wickets, current_rr,
                            required_rr, target, batting_first, final_score,
                            venue_avg, projected_score, chase_pressure=None):
        """Create enhanced match state with additional features."""
        team1_strength = self.team_rankings.get(row['Team_1'], 0.5)
        team2_strength = self.team_rankings.get(row['Team_2'], 0.5)

        state = {
            'match_id': row['Match ID'],
            'team1': row['Team_1'],
            'team2': row['Team_2'],
            'city': row['City'],
            'toss_winner': row['Toss_Winner'],
            'toss_decision': row['Toss_Decision'],
            'current_over': over,
            'current_score': score,
            'current_wickets': wickets,
            'current_run_rate': current_rr,
            'required_run_rate': required_rr if required_rr is not None else 0,
            'target': target if target is not None else 0,
            'batting_first': batting_first,
            'toss_winner_team1': 1 if row['Toss_Winner'] == row['Team_1'] else 0,
            'final_result': 1 if row['Winner'] == row['Team_1'] else 0,
            'runs_per_over': score / over if over > 0 else 0,
            'wickets_per_over': wickets / over if over > 0 else 0,
            'team1_strength': team1_strength,
            'team2_strength': team2_strength,
            'venue_avg_score': venue_avg,
            'projected_score': projected_score,
            'score_vs_venue_avg': score / venue_avg if venue_avg > 0 else 1
        }

        if chase_pressure is not None:
            state['chase_pressure'] = chase_pressure

        return state

    def train_models(self, realtime_df, final_scores_df):
        """Train models optimized for T20 cricket."""
        self.encoders = {
            'team_encoder': LabelEncoder(),
            'toss_decision_encoder': LabelEncoder(),
            'city_encoder': LabelEncoder()
        }

        # Combine team1 and team2 columns to fit the encoder with all possible labels
        all_teams = pd.concat(
            [realtime_df['team1'], realtime_df['team2'], final_scores_df['team1'], final_scores_df['team2']]
        ).unique()
        self.encoders['team_encoder'].fit(all_teams)

        # Fit and transform categorical features
        for df in [realtime_df, final_scores_df]:
            df['team1_encoded'] = self.encoders['team_encoder'].transform(df['team1'])
            df['team2_encoded'] = self.encoders['team_encoder'].transform(df['team2'])
            df['toss_decision_encoded'] = self.encoders['toss_decision_encoder'].fit_transform(df['toss_decision'])
            df['city_encoded'] = self.encoders['city_encoder'].fit_transform(df['city'])

        # Enhanced feature set for T20
        self.features = [
            'current_over', 'current_score', 'current_wickets',
            'current_run_rate', 'batting_first', 'team1_encoded',
            'team2_encoded', 'city_encoded', 'toss_winner_team1',
            'toss_decision_encoded', 'required_run_rate', 'target',
            'runs_per_over', 'wickets_per_over', 'team1_strength',
            'team2_strength', 'venue_avg_score', 'projected_score',
            'score_vs_venue_avg'
        ]

        if 'chase_pressure' in realtime_df.columns:
            self.features.append('chase_pressure')

        # Train win probability model
        X_win = realtime_df[self.features]
        y_win = realtime_df['final_result']

        X_win_train, X_win_test, y_win_train, y_win_test = train_test_split(
            X_win, y_win, test_size=0.2, random_state=42, stratify=y_win
        )

        self.win_scaler = StandardScaler()
        X_win_train_scaled = self.win_scaler.fit_transform(X_win_train)
        X_win_test_scaled = self.win_scaler.transform(X_win_test)

        self.win_model = RandomForestClassifier(
            n_estimators=200,
            max_depth=10,
            min_samples_split=5,
            random_state=42
        )
        self.win_model.fit(X_win_train_scaled, y_win_train)

        # Train score prediction model
        X_score = final_scores_df[self.features]
        y_score = final_scores_df['final_score']

        X_score_train, X_score_test, y_score_train, y_score_test = train_test_split(
            X_score, y_score, test_size=0.2, random_state=42
        )

        self.score_scaler = StandardScaler()
        X_score_train_scaled = self.score_scaler.fit_transform(X_score_train)
        X_score_test_scaled = self.score_scaler.transform(X_score_test)

        self.score_model = RandomForestRegressor(
            n_estimators=200,
            max_depth=10,
            min_samples_split=5,
            random_state=42
        )
        self.score_model.fit(X_score_train_scaled, y_score_train)

        # Calculate metrics
        win_accuracy = accuracy_score(y_win_test, self.win_model.predict(X_win_test_scaled))
        score_r2 = r2_score(y_score_test, self.score_model.predict(X_score_test_scaled))
        score_rmse = np.sqrt(mean_squared_error(y_score_test, self.score_model.predict(X_score_test_scaled)))

        return {
            'win_accuracy': win_accuracy,
            'score_r2': score_r2,
            'score_rmse': score_rmse
        }

    from datetime import datetime

    def predict(self,
                team1, team2, city, batting_team,
                current_score, current_wickets, current_over,
                target, toss_winner, toss_decision, batting_first):

        # Determine the bowling team
        bowling_team = team1 if batting_team == team2 else team2

        match_info = {
            'current_over': current_over,
            'current_score': current_score,
            'current_wickets': current_wickets,
            'batting_first': batting_first,
            'team1': team1,
            'team2': team2,
            'city': city,
            'toss_winner': toss_winner,
            'toss_decision': toss_decision,
            'target': target,
        }

        # Prepare input data for prediction
        input_data = self._prepare_prediction_input(match_info)
        input_df = pd.DataFrame([input_data])

        # Scale inputs
        win_input_scaled = self.win_scaler.transform(input_df[self.features])
        score_input_scaled = self.score_scaler.transform(input_df[self.features])

        # Base predictions
        win_prob = self.win_model.predict_proba(win_input_scaled)[0]
        predicted_final_score = self.score_model.predict(score_input_scaled)[0]

        # Adjust predictions based on match situation
        if match_info['batting_first'] == 0 and match_info['target'] is not None:
            win_prob = self._adjust_chase_win_probability(
                win_prob, match_info, predicted_final_score
            )

        # Determine the likely winner
        likely_winner = team1 if win_prob[1] > win_prob[0] else team2

        # Prepare the output in the desired format
        output = {
            'batting_team': batting_team,
            'bowling_team': bowling_team,
            'city': city,
            'current_over': current_over,
            'current_score': current_score,
            'current_wickets': current_wickets,
            'innings': '1st' if batting_first == 1 else '2nd',
            'likely_winner': likely_winner,
            'predicted_final_score': round(predicted_final_score, 1),
            'status': 'success',
            'target': target if target is not None else 0,
            'timestamp': datetime.now().isoformat(),
            'toss_decision': toss_decision,
            'toss_winner': toss_winner,
            'win_probability': {
                team1: win_prob[1],
                team2: win_prob[0],
            }
        }

        return output

    def _adjust_chase_win_probability(self, base_prob, match_info, predicted_score):
        """Adjust win probabilities for chase scenarios in T20."""
        required_rate = (match_info['target'] - match_info['current_score']) / \
                        ((20 - match_info['current_over']) * 6) * 6
        current_rate = match_info['current_score'] / match_info['current_over'] \
            if match_info['current_over'] > 0 else 0

        # Calculate pressure factors
        wickets_factor = (10 - match_info['current_wickets']) / 10
        rate_difference = required_rate - current_rate
        overs_remaining = 20 - match_info['current_over']

        # Adjust probabilities based on chase situation
        if predicted_score >= match_info['target']:
            # Team is on track to chase
            confidence_boost = min(0.2, wickets_factor * 0.2)
            base_prob = np.array([
                base_prob[0] * (1 + confidence_boost),
                base_prob[1] * (1 - confidence_boost)
            ])
        else:
            # Team is behind in chase
            pressure_penalty = min(0.3, (rate_difference / 12) * (1 - wickets_factor))
            base_prob = np.array([
                base_prob[0] * (1 - pressure_penalty),
                base_prob[1] * (1 + pressure_penalty)
            ])

        # Normalize probabilities to sum to 1
        return base_prob / base_prob.sum()

    def _calculate_confidence_scores(self, match_info, win_prob, predicted_score):
        """Calculate confidence scores suitable for T20 cricket."""
        match_progress = match_info['current_over'] / 20
        base_confidence = 0.5 + (match_progress * 0.5)

        if match_info['batting_first'] == 0 and match_info['target'] is not None:
            # Second innings considerations
            runs_needed = match_info['target'] - match_info['current_score']
            overs_left = 20 - match_info['current_over']
            required_rate = runs_needed / (overs_left * 6) * 6
            current_rate = match_info['current_score'] / match_info['current_over'] if match_info[
                                                                                           'current_over'] > 0 else 0
            rate_factor = max(0, 1 - abs(required_rate - current_rate) / 10)
            wickets_factor = (10 - match_info['current_wickets']) / 10

            chase_confidence = (rate_factor + wickets_factor) / 2
            win_confidence = base_confidence * chase_confidence
            score_confidence = base_confidence * (0.7 + 0.3 * chase_confidence)
        else:
            # First innings considerations
            wickets_factor = (10 - match_info['current_wickets']) / 10
            win_confidence = base_confidence * (0.6 + 0.4 * wickets_factor)
            score_confidence = base_confidence * (0.8 + 0.2 * wickets_factor)

        # Additional adjustments based on prediction certainty
        win_margin = abs(win_prob[0] - win_prob[1])
        win_confidence *= (0.8 + 0.2 * win_margin)

        return {
            'win_confidence': round(win_confidence, 3),
            'score_confidence': round(score_confidence, 3)
        }

    def _prepare_prediction_input(self, match_info):
        """Prepare input data for prediction tailored to T20 cricket."""
        current_run_rate = match_info['current_score'] / match_info['current_over'] \
            if match_info['current_over'] > 0 else 0

        # Get team strengths
        team1_strength = self.team_rankings.get(match_info['team1'], 0.5)
        team2_strength = self.team_rankings.get(match_info['team2'], 0.5)

        # Project final score
        projected_score = self._project_score(
            match_info['current_score'],
            match_info['current_over'],
            match_info['current_wickets']
        )

        # Calculate venue average (default if venue not in database)
        venue_avg = 150  # Default average T20 score

        input_data = {
            'current_over': match_info['current_over'],
            'current_score': match_info['current_score'],
            'current_wickets': match_info['current_wickets'],
            'current_run_rate': current_run_rate,
            'batting_first': match_info['batting_first'],
            'team1_encoded': self.encoders['team_encoder'].transform([match_info['team1']])[0],
            'team2_encoded': self.encoders['team_encoder'].transform([match_info['team2']])[0],
            'city_encoded': self.encoders['city_encoder'].transform([match_info['city']])[0],
            'toss_winner_team1': 1 if match_info['toss_winner'] == match_info['team1'] else 0,
            'toss_decision_encoded': self.encoders['toss_decision_encoder'].transform([match_info['toss_decision']])[0],
            'required_run_rate': 0,
            'target': 0,
            'runs_per_over': current_run_rate,
            'wickets_per_over': match_info['current_wickets'] / match_info['current_over'] if match_info[
                                                                                                  'current_over'] > 0 else 0,
            'team1_strength': team1_strength,
            'team2_strength': team2_strength,
            'venue_avg_score': venue_avg,
            'projected_score': projected_score,
            'score_vs_venue_avg': match_info['current_score'] / venue_avg if venue_avg > 0 else 1,
            'chase_pressure': 0
        }

        # Add chase-specific features
        if match_info['target'] is not None:
            balls_remaining = (20 - match_info['current_over']) * 6
            runs_required = match_info['target'] - match_info['current_score']
            if balls_remaining > 0:
                input_data['required_run_rate'] = (runs_required / balls_remaining) * 6
            input_data['target'] = match_info['target']
            input_data['chase_pressure'] = self._calculate_chase_pressure(
                input_data['required_run_rate'],
                current_run_rate,
                match_info['current_wickets']
            )

        return input_data

    def save_models(self, filename):
        """Save the trained models and associated data."""
        model_data = {
            'win_model': self.win_model,
            'score_model': self.score_model,
            'win_scaler': self.win_scaler,
            'score_scaler': self.score_scaler,
            'features': self.features,
            'encoders': self.encoders,
            'team_rankings': self.team_rankings
        }

        with open(filename, 'wb') as f:
            pickle.dump(model_data, f)

    @classmethod
    def load_models(cls, filename):
        """Load saved models and data."""
        with open(filename, 'rb') as f:
            model_data = pickle.load(f)

        predictor = cls()
        predictor.win_model = model_data['win_model']
        predictor.score_model = model_data['score_model']
        predictor.win_scaler = model_data['win_scaler']
        predictor.score_scaler = model_data['score_scaler']
        predictor.features = model_data['features']
        predictor.encoders = model_data['encoders']
        predictor.team_rankings = model_data.get('team_rankings', predictor.team_rankings)

        return predictor
