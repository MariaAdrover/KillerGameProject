package killergameproject;

import staff.Ufo;
import staff.Wall;
import staff.Alive;
import staff.Autonomous;
import staff.KillerShip;
import staff.KillerShoot;
import staff.VisibleObject;

public class KillerRules {

    // AÑADIR TODAS LAS RULES DE KILLERSHOT, CAMBIAR ESTADO
    public static void requestBorderRule(KillerGameProject game, VisibleObject obj, int obstacle) {
        if ((obj instanceof KillerShip)) {
            killerShipBorderRules(game, (KillerShip) obj, obstacle);
        } else if ((obj instanceof Ufo)) {
            killerUfoBorderRules(game, (Ufo) obj, obstacle);
        } else if (obj instanceof KillerShoot) { // Si OBJ es un Shoot
            killerShootBorderRules(game, (KillerShoot) obj, obstacle);
        }
    }

    // AÑADIR TODAS LAS RULES DE KILLERSHOT
    public static void requestRule(KillerGameProject game, VisibleObject obj, VisibleObject obstacle) {
        //if ((obj instanceof KillerShip) {
        if ((obj instanceof KillerShip) && (((KillerShip) obj).getState() == 1)) { // Si OBJ es una nave en estado normal. Si state=2 la nave esta a salvo
            killerShipRules(game, (KillerShip) obj, obstacle);
        } else if ((obj instanceof Ufo)) { // Si OBJ es un ufo
            killerUfoRules(game, obj, obstacle);
        } else if (obj instanceof KillerShoot) { // Si OBJ es un Shoot
            killerShootRules(game, (KillerShoot) obj, obstacle);
        }
    }

    private static void killerShootRules(KillerGameProject game, KillerShoot obj, VisibleObject obstacle) {
        if (((obstacle instanceof KillerShip) && ((KillerShip) obstacle).getState() == 1)) { // Si se chocan dos naves en estado normal se matan las dos >> state 0
            System.out.println("Rules >> nave muerto por shoot / killerShootRules");
            game.eliminateObject(obj);
            game.killShip((KillerShip) obstacle);
            game.sendPointsToPad(obj, 3);
        } else if ((obstacle instanceof Ufo)) { // Si se choca con un Ufo muere la nave
            game.eliminateObject(obstacle);
            game.sendPointsToPad(obj, 1);
        }

    }

    private static void killerShootBorderRules(KillerGameProject game, VisibleObject obj, int obstacle) {
        // 1 --> UP
        // 2 --> RIGHT
        // 3 --> DOWN
        // 4 --> LEFT

        game.eliminateObject(obj);
        System.out.println("rule: eliminado shoot");
    }

    private static void killerShipBorderRules(KillerGameProject game, VisibleObject obj, int obstacle) {
        // 1 --> UP
        // 2 --> RIGHT
        // 3 --> DOWN
        // 4 --> LEFT

        switch (obstacle) {
            case 1:
                game.blockPath((Alive) obj, obstacle);
                break;
            case 2:
                if (game.getRightKiller().getSocket() != null) { // >>>>>>>>>>>>>>>>>>>>> cambiar comprobacion al game
                    game.sendOutSpaceRight((Alive) obj);
                } else {
                    //game.sendLeft((Alive) obj);
                    game.blockPath((Alive) obj, obstacle);
                }
                break;
            case 3:
                game.blockPath((Alive) obj, obstacle);
                break;
            case 4:
                if (game.getRightKiller().getSocket() != null) {
                    game.sendOutSpaceLeft((Alive) obj);
                } else {
                    //game.sendRight((Alive) obj);
                    game.blockPath((Alive) obj, obstacle);
                }
                break;

        }
    }

    private static void killerShipRules(KillerGameProject game, KillerShip obj, VisibleObject obstacle) {
        if (((obstacle instanceof KillerShip) && ((KillerShip) obstacle).getState() == 1)) { // Si se chocan dos naves en estado normal se matan las dos >> state 0
            System.out.println("Rules >> nave muerto por nave / killerShipRules");
            game.killShip(obj);
            game.killShip((KillerShip) obstacle);
        } else if ((obstacle instanceof Ufo)) { // Si se choca con un Ufo muere la nave
            System.out.println("Rules >> nave muerto por Ufo / killerShipRules");
            game.killShip(obj);
        }

        /*
        if (obstacle instanceof KillerShip && ((KillerShip)obj).getState() == 1) {
            game.killShip(obj);
            game.killShip(obstacle);
        } else if ((obstacle instanceof Ufo) && ((KillerShip)obj).getState() == 1) {
            game.killShip(obj);
        }*/
    }

    private static void killerUfoBorderRules(KillerGameProject game, VisibleObject obj, int obstacle) {
        // 1 --> UP
        // 2 --> RIGHT
        // 3 --> DOWN
        // 4 --> LEFT

        switch (obstacle) {
            case 1:
                game.boiiiingStatic((Alive) obj);
                break;
            case 2:
                if (game.getRightKiller().getSocket() != null) { //pasar comprobacion al metodo del gamekiller
                    game.sendOutSpaceRight((Alive) obj);
                } else {
                    game.sendLeft((Alive) obj);
                }
                break;
            case 3:
                game.boiiiingStatic((Alive) obj);
                break;
            case 4:
                if (game.getLeftKiller().getSocket() != null) {
                    game.sendOutSpaceLeft((Alive) obj);
                } else {
                    game.sendRight((Alive) obj);
                }
                break;
        }

    }

    private static void killerUfoRules(KillerGameProject game, VisibleObject obj, VisibleObject obstacle) {
        if (obstacle instanceof KillerShip && ((KillerShip) obstacle).getState() == 1) { // Si el Ufo choca con una nave en estado normal, mata a la nave >> state 0
            System.out.println("Rules >> nave muerto por Ufo / killerUfoRules");
            game.killShip((KillerShip) obstacle);
        } else if ((obstacle instanceof Ufo)) {
            game.boiiiingAlive((Alive) obj, (Alive) obstacle);
        }

    }
}
