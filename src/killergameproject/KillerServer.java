package killergameproject;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class KillerServer implements Runnable{

    private KillerGameProject game;
    private ServerSocket serverSocket;
    private int port;

    public KillerServer(KillerGameProject game) {
        this.game = game;
        this.port = 8000;
        // Asigna automáticamente el puerto 8000
        // Si está ocupado va incrementando en 1 hasta que encuentra uno libre
        autoConfigurePort();
        // En v0.2 he puesto pantalla para configurar
    }

    private void autoConfigurePort() {
        while (this.serverSocket == null) {
            try {
                this.serverSocket = new ServerSocket(port);
            } catch (IOException ex) {
                System.out.println("KServer: IOexception en setServerSocket");
                System.out.println("KServer --> " + ex);
                this.port++;
            }
        }
    }

    private void waitForConnectionRequests() throws IOException {
        //DONDE PONGO EL CATCH, EN EL RUN O AQUI?
        Socket socket = serverSocket.accept();
        System.out.println("Server: connection request from " + socket.getInetAddress().getHostAddress());
        ConnectionHandler connectionHandler = new ConnectionHandler(this.game, socket); //He de guardarlos en el KillerGame?
        Thread t = new Thread(connectionHandler);
        t.start();
    }

    @Override
    public void run() {

        while (true) {
            System.out.println("Server: Waiting connection request...");
            try {
                waitForConnectionRequests();
            } catch (IOException e) {
                System.out.println("KServer: IOexception en run");
                System.out.println(e.getMessage());
            }

            try {
                Thread.sleep(200);
            } catch (InterruptedException ex) {
                System.out.println("KServer: exception sleep");
                System.out.println(ex.getMessage());
            }
        }
    }

    // ====================================================================
    // ========================  Getters & Setters ========================
    // ====================================================================
    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public ServerSocket getServerSocket() {
        return serverSocket;
    }
}
