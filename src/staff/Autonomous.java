package staff;

import killergameproject.KillerGameProject;

public abstract class Autonomous extends Alive {
    
    public Autonomous (KillerGameProject game, int state, double x, double y, int width, int height, double vX, double vY, double v) {
        super(game, state, x, y, width, height, vX, vY, v);
    }

    
}
