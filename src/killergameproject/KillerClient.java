package killergameproject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;

public class KillerClient implements Runnable {

    private KillerGameProject game;
    private VisualHandler vh;

    public KillerClient(KillerGameProject game, VisualHandler vh) {
        this.game = game;
        this.vh = vh;
    }

    private void sendRequest(PrintWriter out) {
        if (this.vh == game.getLeftKiller()) {
            System.out.println("KillerClient: solicito conexion con pc izquierdo");
            out.println("fromR" + game.getServer().getPort());
        } else {
            System.out.println("KillerClient: solicito conexion con pc derecho");
            out.println("fromL" + game.getServer().getPort());
        }

    }

    private void setVHsocket(Socket socket, String response) {
        System.out.println("KillerClient: recibido del ClientHandler: " + response);
        if (this.vh.getSocket() == null && response.equalsIgnoreCase("ok")) {
            this.vh.configure(socket);
            this.vh.setPort(socket.getPort());
        } else {
            System.err.println("KC: No se ha recibido confirmación del servidor, No se puede asignar el socket al VH");
        }
    }

    private void connect() {
        Socket socket = null;
        System.out.println("KC: SOLICITO CONEXION a " + this.vh.getIp() + " port " + this.vh.getPort());
        try {
            socket = new Socket(this.vh.getIp(), this.vh.getPort());
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);

            // Enviar solicitud al ClientHandler para que asigne el socket a uno de los VisualHandlers
            sendRequest(out);

            // Esperar confirmacion del clientHandler del servidor 
            String response = in.readLine();
            setVHsocket(socket, response);
            
            // Cuando todo esta listo mandamos el 1r mensaje de control
            this.vh.sendControlMessage();
        } catch (IOException ex) {
            System.out.println("KC: IOException");
            System.err.println(ex);
        } catch (Exception ex) {
            System.out.println("KC: Exception");
            System.err.println(ex);
        }
    }

    @Override
    public void run() {

        while (true) {
            if (this.vh.getIp() != null && this.vh.getSocket() == null) {
                connect();
            }
            
            if (this.vh.getSocket() != null) {
                System.out.println(System.currentTimeMillis() - this.vh.getControlTime());
                if (System.currentTimeMillis() - this.vh.getControlTime() > 1000) {
                    System.out.println(System.currentTimeMillis() - this.vh.getControlTime());
                    this.vh.killSocket();
                    System.out.println(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> KC killed socket");
                }
            }

            try {
                Thread.sleep(200);
            } catch (InterruptedException ex) {
                Logger.getLogger(KillerClient.class.getName()).log(Level.SEVERE, null, ex);
            }

        }
    }

}
