package killergameproject;

import staff.Ufo;
import java.awt.Color;
import java.awt.Container;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.Socket;
import java.util.ArrayList;
import javafx.embed.swing.JFXPanel;
import javax.swing.JButton;
import javax.swing.JFrame;
import static javax.swing.JFrame.EXIT_ON_CLOSE;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import staff.Alive;
import staff.KillerShip;
import staff.KillerShoot;
import staff.VisibleObject;

/**
 *
 * @author miaad
 */
public class KillerGameProject extends JFrame implements ActionListener {

    private JFrame configurationWindow;
    private KillerServer server;
    private VisualHandler leftKiller;
    private VisualHandler rightKiller;
    private ArrayList<KillerPad> pads;
    private ArrayList<VisibleObject> objects;
    private Viewer viewer;

    private JButton setLeft;
    private JTextField leftIp;
    private JTextField leftPort;

    private JButton setRight;
    private JTextField rightIp;
    private JTextField rightPort;

    private JButton play;

    public KillerGameProject() {
        // Crear pantalla de configuracion
        this.createWindow();

        // Crear server
        this.server = new KillerServer(this);

        // Crear VisualHandlers con socket null  y sin ip
        this.leftKiller = new VisualHandler(this);
        this.rightKiller = new VisualHandler(this);

        // Crear array para los KillerPads
        this.pads = new ArrayList<>();

        // Crear viewer
        this.viewer = new Viewer(this);

        Container c = this.getContentPane();
        c.add(viewer);

        // Crear objetos iniciales
        this.objects = new ArrayList<>();
        prepareGameObjects();

        this.setTitle("01001011.01000111");
        this.setDefaultCloseOperation(EXIT_ON_CLOSE);
        this.setLocation(0, 0);
        //this.setResizable(false);
        this.pack();
    }

    public void blockPath(Alive obj, int obstaclePosition) {
        // para que un obj no pueda pasar a traves de un muro
        // Poner el metodo en el objeto?
        switch (obstaclePosition) {
            case 1:
                obj.setY(0);
                //this.sendVibrate(obj);
                break;
            case 2:
                obj.setX(this.viewer.getWidth() - obj.getWidth());
                break;
            case 3:
                obj.setY(this.viewer.getHeight() - obj.getHeight());
                //this.sendVibrate(obj);
                break;
            case 4:
                obj.setX(0);
                break;
        }
    }

    public void boiiiingStatic(Alive obj) {
        // rebotar arriba y abajo
        // Poner el metodo en el objeto?
        obj.setvY(-obj.getvY());
    }

    public void boiiiingAlive(Alive obj, Alive obstacle) {
        // rebotar arriba y abajo
        // Poner el metodo en el objeto?
        if ((obj.getX() < obstacle.getX() && obj.getvX() > 0) || (obstacle.getX() < obj.getX() && obstacle.getvX() > 0)) { // mirar >= y <=
            obj.setvX(-obj.getvX());
            obstacle.setvX(-obstacle.getvX());
        }
        if ((obj.getY() < obstacle.getY() && obj.getvY() > 0) || (obstacle.getY() < obj.getY() && obstacle.getvY() > 0)) { // mirar >= y <=
            obj.setvY(-obj.getvY());
            obstacle.setvY(-obstacle.getvY());
        }

    }

    public void createNewKillerPad(Socket socket, String request) {
        // crea nuevo KP y nave
        KillerPad pad = new KillerPad(this, socket);
        this.pads.add(pad);
        (new Thread(pad)).start();
        // Para usar el método estático. No se puede hacer directo?
        pad.askForShip(request);
    }

    public void createNewKillerShip(String id, String user, Color color) {
        // Crea KS para KP
        KillerRadio.playEffect("sound/yoda.wav");
        KillerShip ship = new KillerShip(this, 2, 200, 100, 55, 55, 0, 0, 3, id, color, user);

        this.objects.add(ship);
        (new Thread(ship)).start();

        System.out.println("KillerGame: creo nave con ID " + id);
    }

    public KillerShip getShipById(String shipID) {
        KillerShip ship = null;

        for (int i = 0; i < this.objects.size(); i++) {
            if ((this.objects.get(i) instanceof KillerShip) && ((KillerShip) this.objects.get(i)).getId().equalsIgnoreCase(shipID)) {
                ship = (KillerShip) this.objects.get(i);
            }
        }

        return ship;
    }

    public KillerPad getPadById(String padId) {
        KillerPad pad = null;
        for (int i = 0; i < this.pads.size(); i++) {
            if (this.pads.get(i).getShipID().equalsIgnoreCase(padId)) {
                pad = this.pads.get(i);
            }
        }
        return pad;
    }

    public void eliminateObject(VisibleObject obj) {
        // Cambio el estado del objeto para finalizar su hilo
        obj.setState(-1);
        // Elimino el objeto del array de objetos
        this.objects.remove(obj);
    }

    public void eliminatePad(KillerPad pad) {
        // Elimino el pad del array de pads
        this.pads.remove(pad);
    }

    public void killShip(KillerShip ship) {
        KillerRadio.playEffect("sound/rooster.wav");
        // cambiamos el estado de la nave, pero no la eliminamos
        // conservamos el las propiedades del objeto y lo conservamos en el array
        // conservamos el pad
        // el estado cambia a 0, por lo que la nave no se pintará
        System.out.println("KG: mato la nave");
        ship.setState(0);
        sendKilledToPad(ship);
    }

    public void moveShip(String shipId, boolean[] movement) {
        KillerShip ship;
        for (int i = 0; i < this.objects.size(); i++) {
            if ((this.objects.get(i)) instanceof KillerShip && ((KillerShip) this.objects.get(i)).getId().equalsIgnoreCase(shipId)) {
                ship = (KillerShip) this.objects.get(i);
                ship.setMovement(movement);
            }
        }
    }

    public void reviveShip(KillerShip ship, int state) {
        // en principio state=2, a salvo
        // para que la nave esté a salvo
        ship.setState(state);
        if (state == 2) {
            ship.resetSafeTimer();
        }
    }

    private void sendKilledToPad(KillerShip ship) {
        // Para que el KP envíe al mando la notificación de que la nave es eliminada
        KillerPad pad = null;
        for (int i = 0; i < this.pads.size(); i++) {
            if (this.pads.get(i).getShipID().equalsIgnoreCase(ship.getId())) {
                pad = this.pads.get(i);
            }
        }

        if (pad == null) {
            // si no tengo el KillerPad
            if (this.rightKiller.getSocket() != null) {
                // le digo al VisualHandler derecho que envie un mensaje para buscar el pad y mandar el mensaje
                System.out.println("KG: no tengo el pad de la nave, reenvio el mensaje");
                this.rightKiller.sendKilledToPad(this.server.getPort(), ship.getId());
            }
        } else {
            // si tengo el KillerPad mando el mensaje "ded" al pad
            pad.killShip();
        }
    }

    public void sendPointsToPad(KillerShoot shoot, int points) {
        // Para que el KP envíe al mando la notificación de que la nave ha conseguido puntos
        KillerPad pad = null;
        for (int i = 0; i < this.pads.size(); i++) {
            if (this.pads.get(i).getShipID().equalsIgnoreCase(shoot.getId())) {
                pad = this.pads.get(i);
            }
        }

        if (pad == null) {
            // si no tengo el KillerPad
            if (this.rightKiller.getSocket() != null) {
                // le digo al VisualHandler derecho que envie un mensaje para buscar el pad y mandar el mensaje
                System.out.println("KG: no tengo el pad de la nave, reenvio el mensaje");
                this.rightKiller.sendPointsToPad(this.server.getPort(), shoot.getId(), points);
            }
        } else {
            // si tengo el KillerPad mando el mensaje "pnt" al pad con los puntos conseguidos
            pad.sendPoints(points);
        }

    }

    public void sendLeft(Alive obj) {
        // Si el handler derecho no esta conectado mando los objetos a la parte izquierda de la pantalla
        obj.setX(0);
    }

    public void sendRight(Alive obj) {
        // Si el handler izquierdo no esta conectado mando los objetos a la parte derecha de la pantalla
        obj.setX(this.viewer.getWidth() - obj.getWidth());
    }

    public void sendOutSpaceLeft(Alive obj) {
        //mandar a la pantalla de la izquierda
        this.leftKiller.sendAlive(obj);
        this.eliminateObject(obj);
    }

    public void sendOutSpaceRight(Alive obj) {
        //mandar a la pantalla de la derecha
        this.rightKiller.sendAlive(obj);
        this.eliminateObject(obj);
    }

    public void shoot(String shipId) {

        // La nave crea el disparo si su estado es 1
        // Mejor hacer la comprobación de estado aquí?
        KillerShoot shoot = null;
        for (int i = 0; i < this.objects.size(); i++) {
            if ((this.objects.get(i) instanceof KillerShip)
                    && ((KillerShip) this.objects.get(i)).getId().equalsIgnoreCase(shipId)) {
                shoot = ((KillerShip) this.objects.get(i)).shoot();
            }
        }

        // Inicio el hilo en el constructor del objeto si la nave dispara
        // Si su estado no es 1 no dispara
        if (shoot != null) {
            this.objects.add(shoot);
        }
    }

    public void startGame() {
        // Server
        (new Thread(this.server)).start();
        // Visual handlers
        (new Thread(this.leftKiller)).start();
        (new Thread(this.rightKiller)).start();
        // Viewer
        (new Thread(this.viewer)).start();
        // Objects
        for (Object o : this.objects) {
            if (o instanceof Alive) {
                (new Thread((Alive) o)).start();
            }
        }
        // Music
        JFXPanel jfxPanel = new JFXPanel();
        //KillerRadio.playAudio("sound/8-bit-Arcade4.wav");
        this.setVisible(true);
        configurationWindow.setVisible(true);
    }

    public synchronized void testCollision(Alive obj) {
        // Testear choques con los bordes
        // >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> Hacer con objetos Wall
        int crashBorder = testBorders(obj);
        if (crashBorder != 0) {
            KillerRules.requestBorderRule(this, obj, crashBorder);
        }

        // Testear choques con otrs objetos
        VisibleObject obstacle;
        for (int i = 0; i < this.objects.size(); i++) {
            obstacle = this.objects.get(i);
            if (obj != obstacle && obj.intersect(obstacle)) {
                KillerRules.requestRule(this, obj, obstacle);
            }
        }
    }

    public void welcomeKillerShip(int state, double x, double y, int width, int height, double vX, double vY, double v, String id, Color color, String user) {
        KillerShip ship = new KillerShip(this, state, x, y, width, height, vX, vY, v, id, color, user);
        // Crear nave que viene de otro pc
        // para conservar la direccion del mando...
        ship.setAxisValues();

        this.objects.add(ship);
        (new Thread(ship)).start();
    }

    public void welcomeUfo(int x, int y, int width, int height, double vX, double vY, double v) {
        // Crear ufo que viene de otro pc
        Ufo ufo = new Ufo(this, 1, x, y, width, height, vX, vY, v);
        this.objects.add(ufo);
        (new Thread(ufo)).start();
    }

    private void createWindow() {
        Font bold = new Font("Arial", Font.BOLD, 17);
        Font plain = new Font("Courier New", Font.PLAIN, 17);
        
        configurationWindow = new JFrame("Configuration");
        Container c = configurationWindow.getContentPane();
        JPanel pane = new JPanel();

        JLabel leftIpLabel = new JLabel("Left VH IP:");
        leftIpLabel.setFont(bold);
        pane.add(leftIpLabel);

        this.leftIp = new JTextField(16);
        leftIp.setFont(plain);
        pane.add(leftIp);

        JLabel leftPortLabel = new JLabel("Left VH Port:");
        leftPortLabel.setFont(bold);
        pane.add(leftPortLabel);

        this.leftPort = new JTextField(6);
        leftPort.setFont(plain);
        pane.add(leftPort);

        this.setLeft = new JButton("Set Left VH");
        setLeft.setFont(bold);
        setLeft.addActionListener(this);
        pane.add(setLeft);

        JLabel rightIpLabel = new JLabel("Right VH IP:");
        rightIpLabel.setFont(bold);
        pane.add(rightIpLabel);

        this.rightIp = new JTextField(16);
        rightIp.setFont(plain);
        pane.add(rightIp);

        JLabel rightPortLabel = new JLabel("Right VH Port:");
        rightPortLabel.setFont(bold);
        pane.add(rightPortLabel);

        this.rightPort = new JTextField(6);
        rightPort.setFont(plain);
        pane.add(rightPort);

        this.setRight = new JButton("Set Right VH");
        setRight.setFont(bold);
        setRight.addActionListener(this);
        pane.add(setRight);

        this.play = new JButton("Play");
        play.setFont(bold);
        play.addActionListener(this);
        pane.add(play);

        c.add(pane);

        configurationWindow.setDefaultCloseOperation(HIDE_ON_CLOSE);
        configurationWindow.setSize(300, 300);
        configurationWindow.setLocation(0, 0);
        configurationWindow.setResizable(false);
        configurationWindow.pack();
    }

    private void prepareGameObjects() {
        //this.objects.add(new Ufo(this, 1, 850, 600, 30, 30, 1, -1, 4));
        //this.objects.add(new Ufo(this, 1, 400, 600, 30, 30, 1, -1, 2));
        //this.objects.add(new Ufo(this, 1, 500, 350, 30, 30, 1, 1, 3));
        this.objects.add(new Ufo(this, 1, 300, 288, 30, 30, -1, -1, 1));
        this.objects.add(new Ufo(this, 1, 155, 400, 30, 30, 1, -1, 2));
        //this.objects.add(new Ufo(this, 1, 400, 378, 30, 30, -1, 1, 2));
        //this.objects.add(new Ufo(this, 1, 900, 800, 30, 30, -1, 1, 1));
        //this.objects.add(new Ufo(this, 1, 500, 666, 30, 30, -1, -1, 5));
    }

    private int testBorders(Alive obj) {
        // Comprobar varias posibilidades a la vez, esquinas
        // >>>>>>>>>>>>>>>>>>>>>>>> esto es muy kk TO DOOOOOOOOOOOOOO
        int crashed = 0; // no choca

        if (obj.getY() < 0) { // 1 --> choca arriba
            crashed = 1;
        } else if (obj.getY() + obj.getHeight() > this.viewer.getHeight()) { // 3 --> choca abajo
            crashed = 3;
        }

        if ((obj.getX() + obj.getWidth() > this.viewer.getWidth()) && (obj.getvX() > 0)) { // 2 --> choca derecha
            crashed = 2;
        } else if ((obj.getX() < 0) && (obj.getvX() < 0)) { // 4 --> choca izquierda
            crashed = 4;
        }

        return crashed;
    }

    @Override
    public void actionPerformed(ActionEvent ae) {
        JButton clicked = (JButton) ae.getSource();

        // Los botones SET permiten cambiar el puerto y la ip en cualquier momento
        // PERO NO mandan al cliente hacer un nuevo intento de conexion
        // Se puede añadir leftKiller.getSocket == null
        if (clicked == this.setLeft) {
            this.leftKiller.setIp(this.leftIp.getText());
            this.leftKiller.setPort(Integer.parseInt(this.leftPort.getText()));
        } else if (clicked == this.setRight) {
            this.rightKiller.setIp(this.rightIp.getText());
            this.rightKiller.setPort(Integer.parseInt(this.rightPort.getText()));
        }
        if (clicked == this.play) {
            // Añadir envio de notificaciones si es necesario
            // Por ahora solo oculta la ventana
            this.configurationWindow.setVisible(false);

        }
    }

    public void showWindowConfiguartion() {
        this.configurationWindow.setVisible(true);
    }

    public static void main(String[] args) {
        KillerGameProject game = new KillerGameProject();
        game.startGame();
    }

    // ====================================================================
    // ========================  Getters & Setters ========================
    // ====================================================================
    public ArrayList<VisibleObject> getObjects() {
        return objects;
    }

    public Viewer getViewer() {
        return viewer;
    }

    public VisualHandler getLeftKiller() {
        return leftKiller;
    }

    public VisualHandler getRightKiller() {
        return rightKiller;
    }

    public KillerServer getServer() {
        return server;
    }

}


/*
    private void addWalls() {
        //Revisar posiciones x, y
        this.objects.add(new Wall(this, 0, 0, this.viewer.getWidth(), 1, 0)); // arriba
        this.objects.add(new Wall(this, this.viewer.getWidth(), 0, 1, this.viewer.getHeight(), 3)); // derecha
        this.objects.add(new Wall(this, 0, this.viewer.getHeight(), this.viewer.getWidth(), 1, 6)); // abajo
        this.objects.add(new Wall(this, 0, 0, 1, this.viewer.getHeight(), 9)); // izquierda
    }
 */
