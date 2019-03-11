package staff;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import killergameproject.KillerGameProject;
import killergameproject.KillerRadio;

public class KillerShip extends Controlled {

    private String id;
    private Color color;
    private double safeTimer;
    private String user;
    private double lastDirectionX;

    public KillerShip(KillerGameProject game, int state, double x, double y, int width, int height, double vX, double vY, double v, String id, Color color, String user) {
        super(game, state, x, y, width, height, vX, vY, v);
        this.id = id;
        this.color = color;
        this.user = user;
        this.lastDirectionX = v;
    }

    public void setMovement(boolean[] movement) {
        up = movement[0];
        down = movement[1];
        left = movement[2];
        right = movement[3];
    }

    private void checkSafeState() {
        if (System.currentTimeMillis() - safeTimer > 5000) {
            state = 1;
        }
    }

    public void resetSafeTimer() {
        this.safeTimer = System.currentTimeMillis();
    }

    public KillerShoot shoot() {
        
        // Music effect
        if (this.state == 1) {
            System.out.println("sonido");
            KillerRadio.playEffect("sound/shoot.wav");
        }

        KillerShoot shoot = null;

        if (this.state == 1) {
            double shootX;
            double shootV = this.v + 1;
            int shootWidth = this.width / 3;
            int shootHeight = this.height / 3;

            if (this.lastDirectionX > 0) {
                shootX = this.x + this.width + 1;
            } else {
                shootX = this.x - shootWidth - 1;
            }
            
            shoot = new KillerShoot(this.game, 1, shootX, this.y + shootHeight, shootWidth, shootHeight, lastDirectionX, 0, shootV, this.id, this.color, this.user);
        }

        return shoot;
    }

    public void setAxisValues() {

        // Poner la v que lleva en cada eje        
        // para conservar la direccion del mando...
        if (vX > 0) {
            this.right = true;
            this.left = false;
        } else if (vX < 0) {
            this.right = false;
            this.left = true;
        }

        if (vY > 0) {
            this.up = false;
            this.down = true;
        } else if (vY < 0) {
            this.up = true;
            this.down = false;
        }
    }

    private void updateDirection() {
        if (up && !down) {
            vY = -v;
        } else if (down && !up) {
            vY = v;
        } else if (up && down || !up && !down) {
            vY = 0;
        }

        if (left && !right) {
            vX = -v;
            // Actualizo direccion para el disparo
            lastDirectionX = -v;
        } else if (right && !left) {
            vX = v;
            // Actualizo direccion para el disparo
            lastDirectionX = v;
        } else if (left && right || !left && !right) {
            vX = 0;
        }
    }

    @Override
    public void render(Graphics2D g) {
        g.setColor(this.color);
        //g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        if (state == 1) { // estado normal
            g.drawString(user, (int) x, (int) y - 5);
            g.fillOval((int) x, (int) y, width, height);
        } else if (state == 2) { // estado a salvo
            g.drawString(user, (int) x, (int) y - 5);
            g.setStroke(new BasicStroke(4));
            g.drawOval((int) x, (int) y, width, height);
        }
    }

    // -1 >> acabar el hilo: muerto y elimino el KillerPad
    // 0 >> muerto pero conectado; la nave no se pinta pero conserva sus propiedades
    // 1 >> normal
    // 2 >> nave a salvo durante 7 segundos
    // 3 
    @Override
    public void run() {
        // this.time = System.nanoTime();
        this.time = System.currentTimeMillis();
        this.safeTimer = System.currentTimeMillis();
        while (state >= 0) {
            if (state == 2) {
                this.checkSafeState();
            }

            try {
                updateDirection();
                move();
                this.game.testCollision(this);
                Thread.sleep(15);
            } catch (InterruptedException ex) {

            }
        }

    }

    // ====================================================================
    // ========================  Getters & Setters ========================
    // ====================================================================
    public String getId() {
        return id;
    }

    public Color getColor() {
        return color;
    }

    public String getUser() {
        return user;
    }

}
