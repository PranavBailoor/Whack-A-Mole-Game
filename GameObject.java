import java.awt.*;

public abstract class GameObject {
    protected int x, y, width, height;
    protected boolean active;
    
    public GameObject(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.active = false;
    }
    
    public abstract void draw(Graphics2D g);
    public abstract void update();
    
    public boolean contains(Point p) {
        return new Rectangle(x, y, width, height).contains(p);
    }
}
