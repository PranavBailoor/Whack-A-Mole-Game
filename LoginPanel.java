import java.awt.*;
import javax.swing.*;

public class LoginPanel extends JPanel {
    private final JTextField usernameField;
    private final JPasswordField passwordField;
    private final JButton loginButton;
    private final JButton registerButton;
    private final JLabel messageLabel;
    private final PlayerManager playerManager;
    
    public LoginPanel(JPanel mainPanel, CardLayout cardLayout, PlayerManager playerManager) {
        this.playerManager = playerManager;
        setLayout(null);
        setBackground(new Color(50, 50, 50));
        
        JLabel titleLabel = new JLabel("Whack-A-Mole", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 40));
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setBounds(0, 50, 800, 50);
        add(titleLabel);
        
        JLabel usernameLabel = new JLabel("Username:");
        usernameLabel.setForeground(Color.WHITE);
        usernameLabel.setBounds(200, 150, 100, 30);
        add(usernameLabel);
        
        usernameField = new JTextField();
        usernameField.setBounds(320, 150, 200, 30);
        add(usernameField);
        
        JLabel passwordLabel = new JLabel("Password:");
        passwordLabel.setForeground(Color.WHITE);
        passwordLabel.setBounds(200, 200, 100, 30);
        add(passwordLabel);
        
        passwordField = new JPasswordField();
        passwordField.setBounds(320, 200, 200, 30);
        add(passwordField);
        
        loginButton = new JButton("Login");
        loginButton.setBounds(250, 280, 100, 40);
        loginButton.addActionListener(e -> handleLogin(mainPanel, cardLayout));
        add(loginButton);
        
        registerButton = new JButton("Register");
        registerButton.setBounds(400, 280, 100, 40);
        registerButton.addActionListener(e -> handleRegister());
        add(registerButton);
        
        messageLabel = new JLabel("");
        messageLabel.setForeground(Color.RED);
        messageLabel.setBounds(200, 340, 400, 30);
        add(messageLabel);
    }
    
    private void handleLogin(JPanel mainPanel, CardLayout cardLayout) {
        String username = usernameField.getText();
        String password = new String(passwordField.getPassword());
        
        if (playerManager.login(username, password)) {
            messageLabel.setText("Login successful!");
            messageLabel.setForeground(Color.GREEN);
            SwingUtilities.invokeLater(() -> {
                cardLayout.show(mainPanel, "game");
            });
        } else {
            messageLabel.setText("Invalid credentials!");
            messageLabel.setForeground(Color.RED);
        }
    }
    
    private void handleRegister() {
        String username = usernameField.getText();
        String password = new String(passwordField.getPassword());
        
        if (playerManager.register(username, password)) {
            messageLabel.setText("Registration successful!");
            messageLabel.setForeground(Color.GREEN);
        } else {
            messageLabel.setText("Username already exists!");
            messageLabel.setForeground(Color.RED);
        }
    }
    
    public String getCurrentPlayer() {
        return usernameField.getText();
    }
}
