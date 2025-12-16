import java.awt.*;
import javax.swing.*;

public class Bomb extends GameObject {
    private Timer hideTimer;
    private Timer animTimer;
    private final GamePanel gamePanel;
    private final int baseY;
    private final int hiddenOffset;
    private int offsetY;
    private final int appearLiftMax = 2;
    private int riseAmount = 0;
    private final int appearRiseExtra = 4;
    private boolean showHole = false;
    private boolean exploded = false;
    private int explosionRadius = 0;
    private final int maxExplosionRadius = 240; // much bigger explosion (increased further)
    private Timer explosionAnimTimer;

    public Bomb(int x, int y, int width, int height, GamePanel gamePanel) {
        super(x, y, width, height);
        this.gamePanel = gamePanel;
        this.baseY = y;
        this.hiddenOffset = Math.max(8, height / 4);
        this.offsetY = hiddenOffset;
    }

    public synchronized void popup(int visibleMs) {
        if (active) return;
        active = true;
        showHole = true;
        startRising();

        // reset any previous explosion state
        exploded = false;
        explosionRadius = 0;
        if (explosionAnimTimer != null) explosionAnimTimer.stop();

        if (hideTimer != null) hideTimer.stop();
        hideTimer = new Timer(visibleMs, e -> {
            startFalling();
        });
        hideTimer.setRepeats(false);
        hideTimer.start();
    }

    private void startRising() {
        if (animTimer != null) animTimer.stop();
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
        animTimer = new Timer(15, e -> {
            if (riseAmount > 0) riseAmount = Math.max(0, riseAmount - 2);
            offsetY = Math.min(hiddenOffset, offsetY + 3);
            gamePanel.repaint();
            if (offsetY >= hiddenOffset && riseAmount == 0) {
                animTimer.stop();
                active = false;
                showHole = false;
                exploded = false;
            }
        });
        animTimer.start();
    }

    // Returns true when a hit was successful (bomb was active)
    public synchronized boolean hit() {
        if (active) {
            if (hideTimer != null) hideTimer.stop();
            // begin animated explosion
            exploded = true;
            explosionRadius = 0;
            if (explosionAnimTimer != null) explosionAnimTimer.stop();
            explosionAnimTimer = new Timer(30, ev -> {
                explosionRadius += 18; // grow quicker and larger
                gamePanel.repaint();
                if (explosionRadius >= maxExplosionRadius) {
                    explosionAnimTimer.stop();
                    exploded = false;
                    explosionRadius = 0;
                }
            });
            explosionAnimTimer.setRepeats(true);
            explosionAnimTimer.start();
            startFalling();
            return true;
        }
        return false;
    }

    public synchronized void forceHide() {
        if (hideTimer != null) hideTimer.stop();
        if (animTimer != null) animTimer.stop();
        if (explosionAnimTimer != null) explosionAnimTimer.stop();
        offsetY = hiddenOffset;
        active = false;
        showHole = false;
        exploded = false;
        explosionRadius = 0;
        gamePanel.repaint();
    }

    public synchronized boolean isActive() { return active; }

    @Override
    public void draw(Graphics2D g) {
        if (showHole) {
            int cx = x + width / 2;
            int cy = baseY + hiddenOffset + height / 2;
            int rx = width / 2 + 10;
            int ry = height / 2 + 6;
            Paint oldPaint = g.getPaint();
            Stroke oldStroke = g.getStroke();
            Composite oldComp = g.getComposite();

            GradientPaint gp = new GradientPaint(cx, cy - ry, new Color(120, 80, 40), cx, cy + ry, new Color(60, 40, 20));
            g.setPaint(gp);
            g.fillOval(cx - rx, cy - ry, rx * 2, ry * 2);

            g.setPaint(oldPaint);
            g.setColor(new Color(40, 40, 40));
            g.fillOval(cx - rx / 2, cy - ry / 4 + 4, rx, ry / 2);

            g.setStroke(new BasicStroke(3));
            g.setColor(new Color(40, 30, 30));
            g.drawOval(cx - rx, cy - ry, rx * 2, ry * 2);

            long now = System.currentTimeMillis();
            double pulse = Math.abs(Math.sin(now / 220.0));
            int baseR = Math.max(width, height) / 2 + 8;
            int radius = baseR + (int) (pulse * 6);
            double t = offsetY / (double) hiddenOffset;
            g.setStroke(new BasicStroke(3));
            float alpha = (float) (0.6f * (1.0 - t));
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, Math.max(0.18f, alpha)));
            g.setColor(new Color(90, 20, 20));
            g.drawOval(cx - radius, cy - radius, radius * 2, radius * 2);

            g.setComposite(oldComp);
            g.setStroke(oldStroke);
            g.setPaint(oldPaint);
        }

        if (active || offsetY < hiddenOffset) {
            double t = offsetY / (double) hiddenOffset;
            int lift = (int) Math.round(appearLiftMax * (1.0 - t));
            int drawY = baseY + offsetY - lift - riseAmount;
            // bomb body
            g.setColor(Color.BLACK);
            g.fillOval(x, drawY, width, height);
            g.setColor(new Color(70, 70, 70));
            g.drawOval(x, drawY, width, height);

            // highlight
            g.setColor(new Color(120, 120, 120, 120));
            g.fillOval(x + 8, drawY + 8, width / 3, height / 4);

            // fuse
            int fx = x + width / 2;
            int fy = drawY - 6;
            g.setColor(new Color(80, 50, 20));
            g.setStroke(new BasicStroke(3));
            g.drawLine(fx, drawY + 2, fx, fy);
            // spark or explosion (animated)
            if (exploded) {
                int cx = x + width / 2;
                int cy = drawY + height / 2;
                Composite oldComp2 = g.getComposite();
                float alpha = Math.max(0f, 1f - (float) explosionRadius / (float) maxExplosionRadius);
                g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
                g.setColor(new Color(255, 180, 50));
                g.fillOval(cx - explosionRadius, cy - explosionRadius, explosionRadius * 2, explosionRadius * 2);
                g.setColor(new Color(255, 120, 20, (int) (alpha * 255)));
                g.setStroke(new BasicStroke(3));
                g.drawOval(cx - explosionRadius, cy - explosionRadius, explosionRadius * 2, explosionRadius * 2);
                g.setColor(new Color(255, 220, 150, (int) (alpha * 200)));
                int spikes = 14; // more spikes for a bigger explosion
                for (int i = 0; i < spikes; i++) {
                    double ang = i * Math.PI * 2 / spikes;
                    int lx = cx + (int) ((explosionRadius + 12) * Math.cos(ang));
                    int ly = cy + (int) ((explosionRadius + 12) * Math.sin(ang));
                    g.setStroke(new BasicStroke(2 + (explosionRadius / 30)));
                    g.drawLine(cx, cy, lx, ly);
                }
                g.setComposite(oldComp2);
            } else {
                long now = System.currentTimeMillis();
                double pulse = Math.abs(Math.sin(now / 120.0));
                int s = 4 + (int) (pulse * 4);
                g.setColor(Color.ORANGE);
                g.fillOval(fx - s / 2, fy - s / 2, s, s);
            }
        }
    }

    @Override
    public void update() { }

    @Override
    public boolean contains(Point p) {
        double t = offsetY / (double) hiddenOffset;
        int lift = (int) Math.round(appearLiftMax * (1.0 - t));
        int drawY = baseY + offsetY - lift - riseAmount;
        if (new Rectangle(x, drawY, width, height).contains(p)) return true;
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
