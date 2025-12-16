import java.awt.*;
import javax.swing.*;

public class Mole extends GameObject {
    private Timer hideTimer;
    private Timer animTimer;
    private final GamePanel gamePanel;
    private final int baseY; // ground y position
    private final int hiddenOffset; // how far below ground when hidden
    private int offsetY; // current vertical offset (0 = fully up)
    private final int appearLiftMax = 2; // how much the mole lifts when appearing (subtle)
    private int riseAmount = 0; // extra upward translation during appear
    private final int appearRiseExtra = 4; // how far the mole moves upward when appearing (subtle)
    private boolean showHole = false; // whether to draw the hole marker at this mole's position

    public Mole(int x, int y, int width, int height, GamePanel gamePanel) {
        super(x, y, width, height);
        this.gamePanel = gamePanel;
        this.baseY = y;
        // reduce how far the mole travels when hidden so movement is very subtle
        this.hiddenOffset = Math.max(8, height / 4);
        this.offsetY = hiddenOffset;
    }

    // Called by GamePanel to pop this mole for a given duration (ms)
    public synchronized void popup(int visibleMs) {
        if (active) return; // already up or animating
        active = true;
        // show a hole marker where this mole appeared
        showHole = true;
        startRising();

        if (hideTimer != null) hideTimer.stop();
        hideTimer = new Timer(visibleMs, e -> {
            // start falling animation when visible duration ends
            startFalling();
        });
        hideTimer.setRepeats(false);
        hideTimer.start();
    }

    private void startRising() {
        if (animTimer != null) animTimer.stop();
        // animate both the vertical offset (coming out of hole) and a small extra rise
        // faster rise: larger steps and shorter delay so mole reaches top quickly
        animTimer = new Timer(15, e -> {
            offsetY = Math.max(0, offsetY - 3);
            if (riseAmount < appearRiseExtra) riseAmount = Math.min(appearRiseExtra, riseAmount + 2);
            gamePanel.repaint();
            if (offsetY == 0 && riseAmount >= appearRiseExtra) {
                animTimer.stop();
            }
        });
        animTimer.start();
    }

    private void startFalling() {
        if (animTimer != null) animTimer.stop();
        // sink: remove extra rise and then push mole down into the hole
        // faster fall: larger steps and shorter delay so mole sinks quickly
        animTimer = new Timer(15, e -> {
            if (riseAmount > 0) riseAmount = Math.max(0, riseAmount - 2);
            offsetY = Math.min(hiddenOffset, offsetY + 3);
            gamePanel.repaint();
            if (offsetY >= hiddenOffset && riseAmount == 0) {
                animTimer.stop();
                active = false;
                // once fully hidden, remove the hole marker
                showHole = false;
            }
        });
        animTimer.start();
    }

    // Returns true when a hit was successful (mole was active)
    public synchronized boolean hit() {
        if (active) {
            // cancel visible timer and start falling immediately
            if (hideTimer != null) hideTimer.stop();
            startFalling();
            return true;
        }
        return false;
    }

    // Force hide without scheduling anything else
    public synchronized void forceHide() {
        if (hideTimer != null) hideTimer.stop();
        if (animTimer != null) animTimer.stop();
        offsetY = hiddenOffset;
        active = false;
        showHole = false;
        gamePanel.repaint();
    }

    public synchronized boolean isActive() {
        return active;
    }

    @Override
    public void draw(Graphics2D g) {
        // draw a stationary, slightly 3D-looking oval hole at the mole's base while requested
        if (showHole) {
            int cx = x + width / 2;
            int cy = baseY + hiddenOffset + height / 2;
            int rx = width / 2 + 10;
            int ry = height / 2 + 6;
            Paint oldPaint = g.getPaint();
            Stroke oldStroke = g.getStroke();
            Composite oldComp = g.getComposite();

            // subtle vertical gradient to give a 3D impression
            GradientPaint gp = new GradientPaint(cx, cy - ry, new Color(160, 110, 70), cx, cy + ry, new Color(80, 50, 30));
            g.setPaint(gp);
            g.fillOval(cx - rx, cy - ry, rx * 2, ry * 2);

            // darker inner ellipse to simulate depth
            g.setPaint(oldPaint);
            g.setColor(new Color(60, 40, 20));
            g.fillOval(cx - rx / 2, cy - ry / 4 + 4, rx, ry / 2);

            // rim
            g.setStroke(new BasicStroke(3));
            g.setColor(new Color(50, 30, 15));
            g.drawOval(cx - rx, cy - ry, rx * 2, ry * 2);

            // pulsing halo placed on the ground to texture it
            long now = System.currentTimeMillis();
            double pulse = Math.abs(Math.sin(now / 220.0));
            int baseR = Math.max(width, height) / 2 + 8;
            int radius = baseR + (int) (pulse * 6);
            double t = offsetY / (double) hiddenOffset;
            g.setStroke(new BasicStroke(3));
            float alpha = (float) (0.6f * (1.0 - t));
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, Math.max(0.18f, alpha)));
            g.setColor(new Color(139, 69, 19));
            g.drawOval(cx - radius, cy - radius, radius * 2, radius * 2);

            g.setComposite(oldComp);
            g.setStroke(oldStroke);
            g.setPaint(oldPaint);
        }

        if (active || offsetY < hiddenOffset) {
            // t=1 when hidden, 0 when fully up
            double t = offsetY / (double) hiddenOffset;
            int lift = (int) Math.round(appearLiftMax * (1.0 - t));
                int drawY = baseY + offsetY - lift - riseAmount;
            g.setColor(new Color(139, 69, 19));
            g.fillOval(x, drawY, width, height);
            g.setColor(Color.BLACK);
            g.setStroke(new BasicStroke(2));
            g.drawOval(x, drawY, width, height);

            g.fillOval(x + 20, drawY + 15, 8, 8);
            g.fillOval(x + 50, drawY + 15, 8, 8);
            // (hole overlay removed to avoid duplicate brown circle; only pulsing ring remains)
            // (halo moved to ground hole so nothing more here)
        }
    }

    @Override
    public void update() {
    }

    @Override
    public boolean contains(Point p) {
        double t = offsetY / (double) hiddenOffset;
        int lift = (int) Math.round(appearLiftMax * (1.0 - t));
        int drawY = baseY + offsetY - lift - riseAmount;
        // check main mole rect
        if (new Rectangle(x, drawY, width, height).contains(p)) return true;

        // also consider the ground hole oval as part of the hitbox while shown
        if (showHole) {
            int cx = x + width / 2;
            int cy = baseY + hiddenOffset + height / 2;
            int rx = width / 2 + 10;
            int ry = height / 2 + 6;
            double dx = p.x - cx;
            double dy = p.y - cy;
            double inEllipse = (dx * dx) / (double) (rx * rx) + (dy * dy) / (double) (ry * ry);
            if (inEllipse <= 1.0) return true;
        }

        return false;
    }
}
