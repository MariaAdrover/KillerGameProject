package staff;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import killergameproject.KillerGameProject;

public class KillerShoot extends Autonomous {

    protected String id;
    protected String user;
    protected Color color;

    public KillerShoot(KillerGameProject game, int state, double x, double y, int width, int height, double dX, double dY, double speed, String shipId, Color color, String user) {
        super(game, state, x, y, width, height, dX, dY, speed);
        this.id = shipId;
        this.color = color;
        this.user = user;
        (new Thread(this)).start();
    }

    public String getId() {
        return id;
    }

    @Override
    public void render(Graphics2D g) {
        g.setColor(color);
        //g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        //g.drawString(user, (int) x, (int) y);
        g.fillOval((int) x, (int) y, width, height);
    }

}
