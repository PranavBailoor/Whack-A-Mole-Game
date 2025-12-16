import java.io.*;
import java.util.HashMap;
import java.util.Map;

public class PlayerManager {
    private static final String SCORES_FILE = "player_scores.txt";
    private final Map<String, PlayerData> players;
    
    public PlayerManager() {
        players = new HashMap<>();
        loadPlayers();
    }
    
    public boolean register(String username, String password) {
        if (players.containsKey(username)) {
            return false;
        }
        players.put(username, new PlayerData(username, password, 0));
        savePlayers();
        return true;
    }
    
    public boolean login(String username, String password) {
        if (!players.containsKey(username)) {
            return false;
        }
        PlayerData player = players.get(username);
        return player.password.equals(password);
    }
    
    public void updateScore(String username, int score) {
        if (players.containsKey(username)) {
            PlayerData player = players.get(username);
            if (score > player.highScore) {
                player.highScore = score;
            }
            savePlayers();
        }
    }
    
    private void savePlayers() {
        try (PrintWriter writer = new PrintWriter(new FileWriter(SCORES_FILE))) {
            for (PlayerData player : players.values()) {
                writer.println(player.username + "," + player.password + "," + player.highScore);
            }
        } catch (IOException e) {
        }
    }
    
    private void loadPlayers() {
        File file = new File(SCORES_FILE);
        if (!file.exists()) return;
        
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length == 3) {
                    players.put(parts[0], new PlayerData(parts[0], parts[1], Integer.parseInt(parts[2])));
                }
            }
        } catch (IOException e) {
        }
    }

    // Return a copy of the players map for read-only use by UI
    public Map<String, PlayerData> getPlayers() {
        return new HashMap<>(players);
    }
    
    public static class PlayerData {
        public String username, password;
        public int highScore;
        
        public PlayerData(String username, String password, int highScore) {
            this.username = username;
            this.password = password;
            this.highScore = highScore;
        }
    }
}
