package killergameproject;

import java.awt.Color;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;
import staff.KillerShip;

public class KillerPad implements Runnable {

    private final KillerGameProject game;
    private Socket socket; // this.localIp = socket.getLocalAddress().getHostAddress(); --- this.shipID = socket.getInetAddress().getHostAddress();
    private boolean connected;
    private String shipID;
    private String localIp;
    private BufferedReader in;
    private PrintWriter out;

    public KillerPad(KillerGameProject game, Socket socket) {
        this.game = game;
        this.socket = socket;
        this.configure(socket);
    }

    // Hace falta que el metodo este en static??
    public void askForShip(String request) { // request >>> "new&user&221&77&183");
        // Pide al game que cree una nave
        String shipId = socket.getInetAddress().getHostAddress();
        // Procesar mensaje
        KillerPad.executePadOrder(this.game, shipId + ">" + request);
    }

    private void configure(Socket socket) {
        try {
            this.localIp = socket.getLocalAddress().getHostAddress();
            this.shipID = socket.getInetAddress().getHostAddress();
            System.out.println("localIp = " + localIp);
            System.out.println("ip movil = " + shipID);
            this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            this.out = new PrintWriter(socket.getOutputStream(), true);
            this.connected = true;
        } catch (IOException ex) {
            System.out.println("KillerPad: ERROR while configuring");
            this.connected = false;
        }
    }

    public void killShip() {
        System.out.println("KP: mando mess ded");
        this.sendMessage("ded");
    }

    public void processMessage(String message) {
        System.out.println(message);

        // Si el mensaje es bye desconecto el pad
        if (message.equalsIgnoreCase("bye")) {
            this.connected = false;
        }

        // si tengo yo la nave compruebo el message y la muevo
        // si no la tengo, reenvio el message
        KillerShip ks = game.getShipById(this.shipID);
        if (ks == null) {
            System.out.println("KillerPad: NO tengo la nave, reenvio el mensaje");
            resendMessage(message);
        } else {
            System.out.println("KillerPad: tengo la nave");
            //KillerPad.executePadOrder(game, message); >>> cambiar para poner executePadOrder antiguo
            KillerPad.executePadOrder(game, this.shipID + ">" + message); // 192.168.1.46>up
        }

    }

    public void sendMessage(String message) {
        out.println(message);
    }

    public void sendMessageToPad(String message) {
        String[] messParts = message.split(":");
        String order = messParts[0];
        switch (order) {
            case "ded":
                this.killShip();
                break;
            case "pnt":
                this.sendPoints(Integer.parseInt(messParts[1]));
                break;
        }
    }

    public void sendPoints(int points) {
        System.out.println("KP: mando mess ded");
        this.sendMessage("pnt" + points);

    }

    public void sendVibrate() {
        String message = "vib";
        this.sendMessage(message);
    }

    private void go() {
        try {
            // cerrar el socket
            this.socket.close();
            this.socket = null;
            this.game.eliminatePad(this);
        } catch (IOException ex) {
            System.out.println("killerPad: error going");
            Logger.getLogger(KillerPad.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    private void resendMessage(String userAction) { //r192.168.168.1.46:8000:192.168.1.35>up
        String message = "r"
                + this.localIp.trim() + ":"
                + this.game.getServer().getPort() + ":"
                + this.shipID.trim() + ">"
                + userAction;

        System.out.println("pad: " + message);
        if (this.game.getRightKiller().getSocket() != null) {
            this.game.getRightKiller().sendMessage(message);
        }
    }

    public static void createNewShip(KillerGameProject game, String shipId, String data) { // mia&dd4db7
        String[] dataParts = data.split("&");
        String user = dataParts[0];
        int h = Integer.parseInt(dataParts[1], 16);
        Color color = new Color(h);
        game.createNewKillerShip(shipId, user, color);
    }

    // Se comprueba antes de llamar al metodo, si el game tiene la nave
    // Al metodo se le pasa el game y un message tipo "192.168.1.46>orden a ejecutar:dato1&dato2..."
    public static void executePadOrder(KillerGameProject game, String message) { // 
        String[] messContent = message.split(">");
        String shipId = messContent[0];
        String data[] = messContent[1].split(":");
        String order = data[0];
        boolean[] movement;
        KillerShip ship;

        switch (order) {
            case "new":
                KillerPad.createNewShip(game, shipId, data[1]);
                break;
            case "bye":
                ship = game.getShipById(shipId);
                game.eliminateObject(ship);
                break;
            case "replay":
                ship = game.getShipById(shipId);
                game.reviveShip(ship, 2);
                break;
            case "shoot": 
                game.shoot(shipId);
                break;
            case "idle":
                movement = new boolean[]{false, false, false, false};
                game.moveShip(shipId, movement);
                break;
            case "right":
                movement = new boolean[]{false, false, false, true};
                game.moveShip(shipId, movement);
                break;
            case "upright":
                movement = new boolean[]{true, false, false, true};
                game.moveShip(shipId, movement);
                break;
            case "downright":
                movement = new boolean[]{false, true, false, true};
                game.moveShip(shipId, movement);
                break;
            case "left":
                movement = new boolean[]{false, false, true, false};
                game.moveShip(shipId, movement);
                break;
            case "upleft":
                movement = new boolean[]{true, false, true, false};
                game.moveShip(shipId, movement);
                break;
            case "downleft":
                movement = new boolean[]{false, true, true, false};
                game.moveShip(shipId, movement);
                break;
            case "up":
                movement = new boolean[]{true, false, false, false};
                game.moveShip(shipId, movement);
                break;
            case "down":
                movement = new boolean[]{false, true, false, false};
                game.moveShip(shipId, movement);
                break;
        }
    }

    @Override
    public void run() {

        while (connected) {
            try {
                String message = in.readLine();
                if (message != null) {
                    this.processMessage(message);
                } else {
                    // Al cerrar la app del mando, recibe null
                    System.out.println("KP: null null");
                    this.processMessage("bye");
                }
            } catch (Exception ex) {
                ex.getMessage();
            }

            try {
                Thread.sleep(50);
            } catch (InterruptedException ex) {
                Logger.getLogger(KillerPad.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        go();
        System.out.println("killerPad: pad disconnected");

        System.out.println("KillerPad >>>> DISCONNECTED");
    }

    // ====================================================================
    // ========================  Getters & Setters ========================
    // ====================================================================
    public Socket getSocket() {
        return socket;
    }

    public String getShipID() {
        return shipID;
    }

    public void setConnected(boolean connected) {
        this.connected = connected;
    }

}
