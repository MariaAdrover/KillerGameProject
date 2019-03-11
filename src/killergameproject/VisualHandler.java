package killergameproject;

import staff.Ufo;
import java.awt.Color;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;
import staff.Alive;
import staff.KillerShip;

public class VisualHandler implements Runnable {

    private final KillerGameProject game;
    private KillerClient client;
    private Socket socket;
    private String ip;
    private int port;
    private String localIp;
    private BufferedReader in;
    private PrintWriter out;
    private long controlTime;

    public VisualHandler(KillerGameProject game) {
        this.game = game;
        this.socket = null;
        this.ip = null;
        this.localIp = null;
        this.in = null;
        this.out = null;
        this.client = new KillerClient(this.game, this);
    }

    public synchronized void configure(Socket socket) {
        try {
            this.socket = socket;
            this.ip = this.socket.getInetAddress().getHostAddress();
            this.in = new BufferedReader(new InputStreamReader(this.socket.getInputStream()));
            this.out = new PrintWriter(this.socket.getOutputStream(), true);
            this.localIp = this.socket.getLocalAddress().getHostAddress();
            // Ponemos el tiempo de control al tiempo actual
            this.controlTime = System.currentTimeMillis();

            if (this == game.getLeftKiller()) { //quitar solo pruebas
                System.out.println("Visual Handler IZQUIERDO: Conectado con " + this.ip);
            } else {
                System.out.println("Visual Handler DERECHO: Conectado con " + this.ip);
            }
        } catch (IOException ex) {
            closeSocket();
        }
    }

    public void killSocket() {
        try {
            this.socket.close();
            this.socket = null;
            this.in = null;
            this.out = null;
            System.out.println("VH: SOCKET MUERTO");
        } catch (Exception ex) {
            Logger.getLogger(VisualHandler.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void sendAlive(Alive obj) {
        if (obj instanceof KillerShip) {
            sendKillerShip((KillerShip) obj);
        } else if (obj instanceof Ufo) {
            sendUfo((Ufo) obj);
        }
    }

    public void sendControlMessage() {
        this.out.println("hola?");
    }

    public void sendKilledToPad(int puerto, String shipId) { // rp192.168.1.46:192.168.1.35>ded
        String message = "r"
                + this.localIp + ":"
                + puerto + ":"
                + "p" // añado la p para indicar que ha de buscar un pad
                + shipId + ">"
                + "ded";
        this.sendMessage(message);
    }

    public void sendMessage(String message) {
        this.out.println(message);
    }

    public void sendPointsToPad(int puerto, String shipId, int points) { // rp192.168.1.46:192.168.1.35>pnt:1
        String message = "r"
                + this.localIp + ":"
                + puerto + ":"
                + "p" // añado la p para indicar que ha de buscar un pad
                + shipId + ">"
                + "pnt" + ":"
                + points;
        this.sendMessage(message);
    }

    private void closeSocket() { // quitar, prueba
        if (this.socket != null) {

            try {
                this.socket.close(); // y si da error al cerrar??
            } catch (IOException ex) {
                System.out.println("vh: error en close.socket");
            }

            this.socket = null;
            this.in = null;
            this.out = null;
        }
    }

    private void executeOrder(String message) {
        String object = message.substring(0, 5);

        System.out.println(object);
        switch (object) {
            case "kship":
                welcomeKillerShip(message.substring(6));
                break;
            case "ufooo":
                welcomeUfo(message.substring(6));
                break;
            default:
                break;
        }

    }

    // Cambiar protocolo solo con & ???
    private void processRelayMessage(String message) { // localIp:port:shipIp>userAction
        // r192.168.1.46:192.168.135>up
        String messContent[] = message.split(">");
        String header[] = messContent[0].split(":");
        String originIp = header[0];
        int puerto = Integer.parseInt(header[1]);
        String shipId = header[2];
        String order = messContent[1];
        //if (!this.localIp.equalsIgnoreCase(originIp) || (this.localIp.equalsIgnoreCase(originIp) && this.game.getServer().getPort() != puerto)) {
        if (!this.localIp.equalsIgnoreCase(originIp) || this.game.getServer().getPort() != puerto) {
            this.controlTime = System.currentTimeMillis();
            if (!shipId.substring(0, 1).equalsIgnoreCase("p")) { // p192.168.1.46>up
                // La orden es para una nave
                // Comprobamos si el pc tiene la nave
                KillerShip ship = game.getShipById(shipId);
                if (ship == null) {
                    // Si el pc no tiene la nave, reenviamos el mensaje
                    this.resendMessage(message);
                } else {
                    // si el pc tiene la nave ejecutamos la orden del mando
                    KillerPad.executePadOrder(game, shipId + ">" + order);  // 192.168.1.46>up
                }
            } else {
                // La orden es para un pad
                // Comprobamos si el pc tiene el pad
                KillerPad pad = game.getPadById(shipId.substring(1));

                if (pad == null) {
                    // Si el pc no tiene la nave, reenviamos el mensaje
                    this.resendMessage(message);
                } else {
                    pad.sendMessageToPad(order);
                }
            }
        } else {
            // Si la ip de origen coincide con la ip de destino no procesamos el mensaje
            System.out.println("VH processmessage: He sido yo que he mandado el mensaje. MENSAJE DESCARTADO");
        }

    }

    private void processMessage(String message) { //rlocalIp:shipIp>userAction

        String mode = message.substring(0, 1);
        String content = message.substring(1);

        switch (mode) {
            case "d"://el mensaje es para este pc
                executeOrder(content);
                break;
            case "r": // r192.168.1.46:192.168.135>up
                processRelayMessage(content);
                break;
            default:
                System.out.println("VisualHandler: PROTOCOL ERROR unknown MessageMode");
                break;
        }
    }

    private void resendMessage(String message) {
        if (game.getRightKiller().getSocket() != null) {
            this.game.getRightKiller().sendMessage("r" + message);
        } else {
            System.out.println("VH Izquierdo: ERROR, el VH derecho esta  desconectado; no se ha podido reenviar el mensaje");
        }
    }

    // >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> AÑADIR TIEMPO DE INVENCIBILIDAD
    private void sendKillerShip(KillerShip ship) {
        double yPercent = (ship.getY() * 100) / this.game.getViewer().getHeight();
        double vX = ship.getX();

        // dkship&2&...
        String message = "d"
                + "kship" + "&"
                + ship.getState() + "&"
                + (int) ship.getX() + "&" //se puede quitar...
                + (int) yPercent + "&" //mando el porcentaje
                + ship.getWidth() + "&"
                + ship.getHeight() + "&"
                + (int) ship.getvX() + "&"
                + (int) ship.getvY() + "&"
                + (int) ship.getV() + "&"
                + ship.getId() + "&"
                + ship.getColor().getRed() + "&"
                + ship.getColor().getGreen() + "&"
                + ship.getColor().getBlue() + "&"
                + ship.getUser();

        sendMessage(message);
    }

    private void sendUfo(Ufo ufo) {
        double yPercent = (ufo.getY() * 100) / this.game.getViewer().getHeight();
        double vX = ufo.getX();

        //quitar la x
        String message = "d"
                + "ufooo" + "&"
                + ufo.getX() + "&"
                + yPercent + "&" //mando el porcentaje
                + ufo.getWidth() + "&"
                + ufo.getHeight() + "&"
                + ufo.getvX() + "&"
                + ufo.getvY() + "&"
                + ufo.getV();

        sendMessage(message);

    }

    private void welcomeKillerShip(String data) {
        // ship parameters: KillerGame0_1 game, int x, int y, int width, int height, double vX, double vY, double v, String id, Color color
        // quitar la x
        String dataFields[] = data.split("&");
        double x;

        int state = Integer.parseInt(dataFields[0]);
        //int x = Integer.parseInt(d[1]);
        double yPercent = Double.parseDouble(dataFields[2]);
        double y = (yPercent * this.game.getViewer().getHeight()) / 100;
        int width = Integer.parseInt(dataFields[3]);
        int height = Integer.parseInt(dataFields[4]);
        double vX = Double.parseDouble(dataFields[5]);
        double vY = Double.parseDouble(dataFields[6]);
        double v = Double.parseDouble(dataFields[7]);
        String id = dataFields[8];
        int r = Integer.parseInt(dataFields[9]);
        int g = Integer.parseInt(dataFields[10]);
        int b = Integer.parseInt(dataFields[11]);
        Color c = new Color(r, g, b); // >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> cambiar a hexadecimal
        String user = dataFields[12];

        if (vX > 0) {
            x = 0;
        } else {
            x = this.game.getViewer().getWidth() - width;
        }

        this.game.welcomeKillerShip(state, x, y, width, height, vX, vY, v, id, c, user);
    }

    private void welcomeUfo(String data) { //rehaceeeer
        // public void welcomeUfo(int x, int y, int width, int height, double vX, double vY, double v)
        // quitar la x
        String d[] = data.split("&");
        int x;

        //int x = Integer.parseInt(d[0]);
        int yPercent = (int) Double.parseDouble(d[1]);
        int y = (yPercent * this.game.getViewer().getHeight()) / 100;
        int width = Integer.parseInt(d[2]);
        int height = Integer.parseInt(d[3]);
        double vX = Double.parseDouble(d[4]);
        double vY = Double.parseDouble(d[5]);
        double v = Double.parseDouble(d[6]);

        if (vX > 0) {
            x = 0;
        } else {
            x = this.game.getViewer().getWidth() - width;
        }

        this.game.welcomeUfo(x, y, width, height, vX, vY, v);
    }

    @Override
    public void run() {

        (new Thread(client)).start();
        while (true) {
            if (this.getSocket() != null) { // Diferencia poner directamente this.socket... >> jumi?
                try {
                    String message = in.readLine();
                    
                    // Mensaje para confirmar que el otro equipo está conectado
                    // Se reinicia el tiempo al confirmar conexión y contesta al otro equipo
                    // KC comprueba los tiempos y nullea socket y demás si no hay conexión
                    // >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> ok pero probar con socket.setSoTiemout
                    if (message.equalsIgnoreCase("hola?")) {
                        this.controlTime = System.currentTimeMillis();
                        this.sendControlMessage();
                    } else {
                        System.out.println("vh: message " + message);
                        this.processMessage(message);
                    }
                } catch (IOException ex) {
                    System.out.println("VH: IOException socket");
                    this.socket = null; // Usar método killSocket?
                    this.game.showWindowConfiguartion();
                } catch (Exception ex) {
                    System.out.println("VH: Exception socket");
                    System.err.println(ex.getMessage());
                }
            }

            try {
                Thread.sleep(100);
            } catch (InterruptedException ex) {
                Logger.getLogger(VisualHandler.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    // ====================================================================
    // ========================  Getters & Setters ========================
    // ====================================================================
    public synchronized Socket getSocket() {
        return socket;
    }

    public synchronized void setSocket(Socket socket) {
        this.socket = socket;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public KillerClient getClient() {
        return client;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public int getPort() {
        return port;
    }

    public long getControlTime() {
        return controlTime;
    }

}
