import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import javax.swing.*;

public class GamePanel extends JPanel {
    private ArrayList<Mole> moles;
    private ArrayList<Bomb> bombs;
    private final JButton startButton;
    private final JButton stopButton;
    private int score;
    private int timeLeft;
    private boolean gameRunning;
    private Timer gameTimer;
    private Timer popupTimer; // controls when moles pop up
    private Timer accelTimer; // gradually decreases popup delay
    private int popupDelay;
    private final int initialPopupDelay = 1500; // ms
    private final int minPopupDelay = 300; // ms
    private final int popupDecrease = 100; // ms every accel step
    private final int visibleDuration = 1000; // how long a mole stays up (ms)
    private final int GAME_DURATION = 60; // seconds (1 minute)
    private final int MAX_ACTIVE_MOLES = 3;
    private final LoginPanel loginPanel;
    private final PlayerManager playerManager;
    private final JPanel parentPanel;
    private final CardLayout parentLayout;
    private final LeaderboardPanel leaderboardPanel;
    private final JLabel scoreLabel;
    private final JLabel timeLabel;
    // Mallet animation state (shake effect)
    private boolean malletActive = false;
    private int malletX = 0;
    private int malletY = 0;
    private int malletTicks = 0;
    private final int malletMaxTicks = 12;
    private final int malletAmplitude = 6;
    private Timer malletTimer;
    
    public GamePanel(JPanel mainPanel, CardLayout cardLayout, LoginPanel loginPanel, PlayerManager playerManager, LeaderboardPanel leaderboardPanel) {
        this.loginPanel = loginPanel;
        this.playerManager = playerManager;
        this.parentPanel = mainPanel;
        this.parentLayout = cardLayout;
        this.leaderboardPanel = leaderboardPanel;
        setLayout(null);
        setBackground(new Color(34, 139, 34));
        
        moles = new ArrayList<>();
        score = 0;
        timeLeft = GAME_DURATION;
        gameRunning = false;
        
        // Create moles in a grid
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                moles.add(new Mole(100 + i * 200, 100 + j * 120, 80, 80, this));
            }
        }
        // Create a Bomb instance for each hole so bombs can appear in the same positions
        bombs = new ArrayList<>();
        for (Mole m : moles) {
            bombs.add(new Bomb(m.x, m.y, m.width, m.height, this));
        }
        
        scoreLabel = new JLabel("Score: 0");
        scoreLabel.setFont(new Font("Arial", Font.BOLD, 20));
        scoreLabel.setForeground(Color.WHITE);
        scoreLabel.setBounds(20, 20, 150, 30);
        add(scoreLabel);
        
        timeLabel = new JLabel(formatTime(timeLeft));
        timeLabel.setFont(new Font("Arial", Font.BOLD, 20));
        timeLabel.setForeground(Color.WHITE);
        timeLabel.setBounds(200, 20, 150, 30);
        add(timeLabel);
        
        startButton = new JButton("Start Game");
        startButton.setBounds(520, 20, 120, 40);
        startButton.addActionListener(e -> startGame());
        add(startButton);
        
        stopButton = new JButton("Stop Game");
        stopButton.setBounds(660, 20, 120, 40);
        stopButton.setEnabled(false);
        stopButton.addActionListener(e -> stopGame());
        add(stopButton);
        
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (gameRunning) {
                    boolean hitAny = false;
                    // Check moles first (positive targets)
                    for (Mole mole : moles) {
                        if (mole.contains(e.getPoint())) {
                            if (mole.hit()) {
                                score += 10;
                                scoreLabel.setText("Score: " + score);
                                hitAny = true;
                            }
                        }
                    }
                    // Then check bombs (penalties)
                    for (Bomb bomb : bombs) {
                        if (bomb.contains(e.getPoint())) {
                            if (bomb.hit()) {
                                score = Math.max(0, score - 20);
                                scoreLabel.setText("Score: " + score);
                                hitAny = true;
                            }
                        }
                    }
                    if (hitAny) triggerMallet(e.getX(), e.getY());
                }
            }
        });
    }
    
    private void startGame() {
        score = 0;
        timeLeft = GAME_DURATION;
        gameRunning = true;
        startButton.setEnabled(false);
        stopButton.setEnabled(true);
        scoreLabel.setText("Score: 0");
        timeLabel.setText(formatTime(timeLeft));
        
        gameTimer = new Timer(1000, e -> {
            timeLeft--;
            timeLabel.setText(formatTime(timeLeft));
            repaint();
            
            if (timeLeft <= 0) {
                stopGame();
            }
        });
        gameTimer.start();
        // Initialize popup control: start with slower pops, then gradually speed up
        popupDelay = initialPopupDelay;
        popupTimer = new Timer(popupDelay, e -> popRandomMole());
        popupTimer.setRepeats(true);
        popupTimer.start();

        // Every 5 seconds, reduce popupDelay until minPopupDelay is reached
        accelTimer = new Timer(5000, e -> {
            if (popupDelay > minPopupDelay) {
                // speed up faster near the end of the game
                int decrease = popupDecrease;
                if (timeLeft <= 10) decrease = popupDecrease * 2;
                if (timeLeft <= 5) {
                    popupDelay = minPopupDelay;
                } else {
                    popupDelay = Math.max(minPopupDelay, popupDelay - decrease);
                }
                if (popupTimer != null) popupTimer.setDelay(popupDelay);
            }
        });
        accelTimer.setRepeats(true);
        accelTimer.start();
    }
    
    private void stopGame() {
        gameRunning = false;
        if (gameTimer != null) {
            gameTimer.stop();
        }
        if (popupTimer != null) {
            popupTimer.stop();
        }
        if (accelTimer != null) {
            accelTimer.stop();
        }
        // ensure no mole or bomb remains active
        for (Mole m : moles) {
            m.forceHide();
        }
        for (Bomb b : bombs) {
            b.forceHide();
        }
        startButton.setEnabled(true);
        stopButton.setEnabled(false);

        playerManager.updateScore(loginPanel.getCurrentPlayer(), score);

        JOptionPane.showMessageDialog(this, "Game Over! Final Score: " + score);

        // show leaderboard
        if (leaderboardPanel != null && parentPanel != null && parentLayout != null) {
            leaderboardPanel.refresh(loginPanel.getCurrentPlayer());
            parentLayout.show(parentPanel, "leaderboard");
        } else if (parentPanel != null && parentLayout != null) {
            parentLayout.show(parentPanel, "login");
        }
    }
    
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        
        for (Mole mole : moles) {
            mole.draw(g2d);
        }
        // draw bombs (if any) over holes but under the mallet
        if (bombs != null) {
            for (Bomb bomb : bombs) {
                bomb.draw(g2d);
            }
        }
        // draw mallet over moles if active (small horizontal shake)
        if (malletActive) {
            int shake = (int) (Math.sin(malletTicks * 1.5) * malletAmplitude);
            int headW = 36;
            int headH = 18;
            int headX = malletX - headW / 2 + shake;
            int headY = malletY - 10;
            g2d.setColor(new Color(160, 82, 45));
            g2d.fillRect(headX, headY, headW, headH);
            g2d.setColor(Color.DARK_GRAY);
            g2d.setStroke(new BasicStroke(4));
            // handle (slightly offset by shake)
            g2d.drawLine(malletX + shake, headY + headH, malletX + 4 + shake, headY + headH + 24);
        }
    }

    private void triggerMallet(int x, int y) {
        if (malletTimer != null) {
            malletTimer.stop();
        }
        malletActive = true;
        malletX = x;
        malletY = y;
        malletTicks = 0;
        malletTimer = new Timer(25, e -> {
            malletTicks++;
            if (malletTicks > malletMaxTicks) {
                malletTimer.stop();
                malletActive = false;
            }
            repaint();
        });
        malletTimer.start();
    }

    // Choose a random inactive hole and pop either a mole or (occasionally) a bomb for the configured visible duration
    private void popRandomMole() {
        if (!gameRunning || moles.isEmpty()) return;
        int activeCount = 0;
        for (Mole m : moles) if (m.isActive()) activeCount++;
        for (Bomb b : bombs) if (b.isActive()) activeCount++;
        if (activeCount >= MAX_ACTIVE_MOLES) return;

        ArrayList<Integer> candidates = new ArrayList<>();
        int total = moles.size();
        for (int i = 0; i < total; i++) {
            if (!moles.get(i).isActive() && !bombs.get(i).isActive()) candidates.add(i);
        }
        if (candidates.isEmpty()) return;
        int chosenHoleIdx = candidates.get((int) (Math.random() * candidates.size()));

        // Bomb chance ramps linearly from 5% (at 15s remaining) up to 15% (at 0s)
        double minChance = 0.05;
        double maxChance = 0.15;
        double bombChance;
        if (timeLeft >= 15) {
            bombChance = minChance;
        } else {
            int t = Math.max(0, timeLeft); // clamp to non-negative
            double frac = (15.0 - t) / 15.0; // 0.0 at 15s -> 1.0 at 0s
            bombChance = minChance + frac * (maxChance - minChance);
        }
        if (Math.random() < bombChance) {
            bombs.get(chosenHoleIdx).popup(visibleDuration);
        } else {
            moles.get(chosenHoleIdx).popup(visibleDuration);
        }
    }

    private String formatTime(int seconds) {
        int m = seconds / 60;
        int s = seconds % 60;
        return String.format("Time: %d:%02d", m, s);
    }
}
