from flask import Flask, request, jsonify
import pandas as pd

app = Flask(__name__)

class PlayerStats:
    def __init__(self, bowler_stats_csv, team_stats_csv, venue_stats_csv):
        self.bowler_stats_csv = bowler_stats_csv
        self.team_stats_csv = team_stats_csv
        self.venue_stats_csv = venue_stats_csv

    def get_player_vs_bowler_stats(self, player_name, bowler_name=None):
        df = pd.read_csv(self.bowler_stats_csv)
        player_data = df[df['striker'] == player_name]
        if bowler_name:
            player_data = player_data[player_data['bowler'] == bowler_name]
        return player_data

    def get_player_vs_team_stats(self, player_name, team_name=None):
        df = pd.read_csv(self.team_stats_csv)
        player_data = df[df['striker'] == player_name]
        if team_name:
            player_data = player_data[player_data['bowling_team'] == team_name]
        return player_data

    def get_player_at_venue_stats(self, player_name, venue_name=None):
        df = pd.read_csv(self.venue_stats_csv)
        player_data = df[df['striker'] == player_name]
        if venue_name:
            player_data = player_data[player_data['venue'] == venue_name]
        return player_data

    def get_player_stats(self, stat_type, player_name, filter_value=None):
        if stat_type == 'vs_bowler':
            return self.get_player_vs_bowler_stats(player_name, filter_value)
        elif stat_type == 'vs_team':
            return self.get_player_vs_team_stats(player_name, filter_value)
        elif stat_type == 'at_venue':
            return self.get_player_at_venue_stats(player_name, filter_value)
        else:
            raise ValueError("Invalid stat type. Use 'vs_bowler', 'vs_team', or 'at_venue'.")

bowler_stats_csv = r"D:\projects\CricPred\data\batter_bowler_stats.csv"
team_stats_csv = r"D:\projects\CricPred\data\player_team_stats.csv"
venue_stats_csv = r"D:\projects\CricPred\data\player_venue_stats.csv"
player_stats = PlayerStats(bowler_stats_csv, team_stats_csv, venue_stats_csv)

@app.route('/stats/<stat_type>', methods=['GET'])
def get_player_stats(stat_type):
    player_name = request.args.get('player_name')
    filter_value = request.args.get('filter_value', None)  # Optional filter (e.g., bowler_name, team_name, venue_name)

    try:
        # Fetch player stats using the PlayerStats class
        stats = player_stats.get_player_stats(stat_type, player_name, filter_value)

        # Convert the result to JSON for easy API consumption
        return stats.to_json(orient='records')
    except Exception as e:
        return jsonify({"error": str(e)}), 400


# Run the Flask app
if __name__ == '__main__':
    app.run(debug=True, port=5050)
