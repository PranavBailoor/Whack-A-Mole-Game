import java.awt.*;
import javax.swing.*;

public class WhackAMole extends JFrame {
    public WhackAMole() {
        setTitle("Whack-A-Mole Game");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(800, 600);
        setLocationRelativeTo(null);
        setResizable(false);
        
        CardLayout cardLayout = new CardLayout();
        JPanel mainPanel = new JPanel(cardLayout);

        PlayerManager playerManager = new PlayerManager();

        LoginPanel loginPanel = new LoginPanel(mainPanel, cardLayout, playerManager);
        LeaderboardPanel leaderboardPanel = new LeaderboardPanel(mainPanel, cardLayout, playerManager);
        GamePanel gamePanel = new GamePanel(mainPanel, cardLayout, loginPanel, playerManager, leaderboardPanel);

        mainPanel.add(loginPanel, "login");
        mainPanel.add(gamePanel, "game");
        mainPanel.add(leaderboardPanel, "leaderboard");

        add(mainPanel);
        setVisible(true);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new WhackAMole());
    }
}


