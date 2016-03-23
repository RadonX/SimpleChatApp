import java.io.*;
import java.net.Socket;

/**
 *
 */
public class Client {

    Socket socket;
    DataOutputStream out;
    DataInputStream in;
    BufferedReader inConsole;
    int state = 0;

    public static void main(String [] args){

        String serverName = args[0];
        int port = Integer.parseInt(args[1]);
        final Client client = new Client(serverName, port);

        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                System.out.print("\nBye bye~ ");
                client.state = 1;
                client.close();
            }
        });

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

    public void close(){
        try {
            socket.close();
            in.close();
            out.close();
            inConsole.close();
        }catch (Exception e){
            //System.out.println("Client was already closed.");
        }
    }

    public void start(){
        String serverReply;

        try {
            System.out.print(in.readUTF());//returns "connected to server ...

            while (true){
                serverReply = in.readUTF();
                System.out.print(serverReply);
                if (serverReply.contains("Welcome")) break; // successfully login
                out.writeUTF(inConsole.readLine());
            }
            // start terminal mode
            System.out.print("=> ");

            // Create a Thread to listen to the server
            new ListenToServer().start();
                // information such as "Timed out" can only prompt directly
                // after login, since another thread was not yet created.

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
                                close();break;
                                //throw IOException;
                            }
                        }

                        System.out.println(serverReply);
                    }
                    System.out.print("=> ");

                }
            } catch (IOException e){
                if (state == 0) {
                    // client side works well, server closes the connection
                    System.out.print("\nDisconnect from server");
                    close();
                }
            }
        }
    }


}

