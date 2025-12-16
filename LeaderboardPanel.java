import java.awt.*;
import java.util.ArrayList;
import java.util.Map;
import javax.swing.*;
import javax.swing.table.*;

public final class LeaderboardPanel extends JPanel {
    private final JTable table;
    private final DefaultTableModel tableModel;
    private String highlightedPlayer = null;
    private final JButton backButton;
    private final PlayerManager playerManager;

    public LeaderboardPanel(JPanel parentPanel, CardLayout parentLayout, PlayerManager playerManager) {
        this.playerManager = playerManager;
        setLayout(new BorderLayout());

        JLabel title = new JLabel("Leaderboard", SwingConstants.CENTER);
        title.setFont(new Font("Arial", Font.BOLD, 28));
        add(title, BorderLayout.NORTH);

        tableModel = new DefaultTableModel(new Object[]{"Rank", "Username", "High Score"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };
        table = new JTable(tableModel);
        table.setRowHeight(28);
        table.getTableHeader().setFont(new Font("Arial", Font.BOLD, 16));
        table.getTableHeader().setBackground(new Color(60, 120, 60));
        table.getTableHeader().setForeground(Color.WHITE);
        table.setFont(new Font("Arial", Font.PLAIN, 14));
        table.setFillsViewportHeight(true);
        // center align rank and score
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(SwingConstants.CENTER);
        table.getColumnModel().getColumn(0).setCellRenderer(centerRenderer);
        table.getColumnModel().getColumn(2).setCellRenderer(centerRenderer);

        // alternating row colors and highlight current player via renderer
        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable tbl, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
                Component c = super.getTableCellRendererComponent(tbl, value, isSelected, hasFocus, row, col);
                if (!isSelected) {
                    // highlight selected player row specially, otherwise use alternating rows
                    Object username = tbl.getValueAt(row, 1);
                    if (highlightedPlayer != null && highlightedPlayer.equals(username)) {
                        c.setBackground(new Color(255, 245, 200));
                    } else {
                        c.setBackground(row % 2 == 0 ? new Color(245, 245, 245) : Color.WHITE);
                    }
                }
                setHorizontalAlignment(col == 0 || col == 2 ? SwingConstants.CENTER : SwingConstants.LEFT);
                return c;
            }
        });

        add(new JScrollPane(table), BorderLayout.CENTER);

        backButton = new JButton("Back to Home");
        backButton.addActionListener(e -> parentLayout.show(parentPanel, "login"));
        JPanel bottom = new JPanel();
        bottom.add(backButton);
        add(bottom, BorderLayout.SOUTH);

        refresh();
    }

    public void refresh() { refresh(null); }

    // refresh leaderboard; pass currentPlayer to highlight their row (optional)
    public void refresh(String currentPlayer) {
        highlightedPlayer = currentPlayer;
        Map<String, PlayerManager.PlayerData> players = playerManager.getPlayers();
        ArrayList<PlayerManager.PlayerData> list = new ArrayList<>(players.values());
        list.sort((a, b) -> Integer.compare(b.highScore, a.highScore));

        tableModel.setRowCount(0);
        int rank = 1;
        for (PlayerManager.PlayerData pd : list) {
            tableModel.addRow(new Object[]{rank, pd.username, pd.highScore});
            rank++;
        }

        // highlight the current player if present
        if (currentPlayer != null) {
            for (int r = 0; r < tableModel.getRowCount(); r++) {
                if (currentPlayer.equals(tableModel.getValueAt(r, 1))) {
                    table.setRowSelectionInterval(r, r);
                    // scroll to make selection visible
                    table.scrollRectToVisible(table.getCellRect(r, 0, true));
                    break;
                }
            }
        } else {
            table.clearSelection();
        }
    }
}
