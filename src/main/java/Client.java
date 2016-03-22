import java.io.*;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;

/**
 *
 */
public class Client {

    String name;
    Socket socket;
    DataOutputStream out;
    DataInputStream in;
    BufferedReader inConsole;


    public static void main(String [] args){
        String serverName = args[0];
        int port = Integer.parseInt(args[1]);
        Client client = new Client(serverName, port);
        client.start();
        client.close();
    }

    Client(String serverName, int port){
        // instantiates a Socket object
        System.out.println("Connecting to " + serverName +
                " on port " + port);

        try {
            socket = new Socket(serverName, port);

            // get i/o stream
            out = new DataOutputStream(socket.getOutputStream());
            in = new DataInputStream(socket.getInputStream());
            inConsole = new BufferedReader(new InputStreamReader(System.in));

            name = "Client<" + socket.getLocalSocketAddress() + "> ";

        } catch (IOException e){
            e.printStackTrace();
        }
    }


    public void start(){
        String serverReply;

        try {
            socket.setSoTimeout(180000);//3min //~~~~~~~~~~~ not write

            System.out.print(in.readUTF());//returns "connected to server ...

            while (true){ //while ((line = inConsole.readLine()) != null) {
                serverReply = in.readUTF();
                System.out.print(serverReply);
                if (serverReply.contains("Welcome")) // successfully login
                    break;
                out.writeUTF(inConsole.readLine());
            }

            while (true){
                System.out.print("Command: ");
                out.writeUTF(inConsole.readLine());
                System.out.print(in.readUTF());
            }

        } catch (SocketTimeoutException e) { //~~~~~~~~~~~~~
            System.out.println("Client socket timed out!");
            e.printStackTrace();
        } catch (IOException e){
            System.out.println("Server closes the connection.");
        }

    }

    public void close(){
        try {
            in.close();
            out.close();
            socket.close();
            inConsole.close();
        }catch (Exception e){}
    }

}
