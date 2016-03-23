import java.io.*;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;

/**
 *
 */
public class Client {

    Socket socket;
    DataOutputStream out;
    DataInputStream in;
    BufferedReader inConsole;


    public static void main(String [] args){
        String serverName = args[0];
        int port = Integer.parseInt(args[1]);
        Client client = new Client(serverName, port);
        client.start();
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

        } catch (IOException e){
            e.printStackTrace();
        }
    }


    public void start(){
        String serverReply;

        try {
            System.out.print(in.readUTF());//returns "connected to server ...

            while (true){ //while ((line = inConsole.readLine()) != null) {
                serverReply = in.readUTF();
                System.out.print(serverReply);
                if (serverReply.contains("Welcome")) break; // successfully login
                out.writeUTF(inConsole.readLine());
            }
            // start terminal mode
            System.out.print("=> ");

            // create a Thread to listen to the server
            new ListenToServer().start();

            //~~~~ String msg = scan.nextLine();
            while (true){
                out.writeUTF(inConsole.readLine());
            }

        } catch (IOException e){
            // connection lost
        } catch (Exception e) {
            e.printStackTrace();
            close();
        }

    }

    private class ListenToServer extends Thread{

        public void run(){
            String serverReply;
            try{
                while (true){
                    serverReply = in.readUTF();
                    // server must reply something after every input,
                    // otherwise the terminal cannot prompt "=>"

                    if (serverReply.length() != 0){
                        if (serverReply.startsWith("\n")) { // server prompt a reply
                            // we use "\n" to tell whether a message is returned after a command,
                            // and "\n[" to tell whether it is message from other clients.
                            // therefore server should not reply to client's command with these formats.
                            // otherwise, should implement io with ObjectInput/OutputStream
                            if (serverReply.startsWith("\nT")){ // timeout
                                System.out.print(serverReply);
                                close();
                                break;
                            }
                        }

                        System.out.println(serverReply);
                    }
                    System.out.print("=> ");

                }
            } catch (IOException e){
                // connection lost.
                System.out.println("You're logged out.");
                close();
            }
        }
    }

    public void close(){
        try {
            in.close();
            out.close();
            socket.close();
            inConsole.close();
        }catch (Exception e){
            System.out.println("Client was already closed.");
        }
    }

}
