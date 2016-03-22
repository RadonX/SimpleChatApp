import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Scanner;

public class Server{

    private ServerSocket serverSocket;
    private Authenticator authenticator;
    SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");

    class LoginList {
        // since there are not many users, list is OK
        // in the order of login time

        ArrayList<String> userList;
        ArrayList<ClientThread> clientThreadList;

        LoginList(){
            userList = new ArrayList<String>();
            clientThreadList = new ArrayList<ClientThread>();
        }

        public synchronized void put(ClientThread clientThread, String user){
            userList.add(user);// == clientThread.user
            clientThreadList.add(clientThread);
        }

        public synchronized void remove(ClientThread clientThread){
            remove(loginList.clientThreadList.indexOf(clientThread));
        }
        public synchronized void remove(int ind){
            userList.remove(ind);
            clientThreadList.remove(ind);
        }

    }

    //ArrayList<ClientThread> clientThreadList = new ArrayList<ClientThread>();
    LoginList loginList;


    public static void main(String [] args) {
        int port = Integer.parseInt(args[0]);
        Server server = new Server(port);
        server.start();
        server.close();
    }

    public Server(int port) {
        // create a server socket that listens on a port
        try {
            serverSocket = new ServerSocket(port);
            serverSocket.setSoTimeout(600000);//10min
        } catch (IOException e) {
            System.err.println("Could not listen on port " + port);
            System.exit(-1);
        }
        authenticator = new Authenticator("user_pass.txt");
        loginList = new LoginList();
    }

    public void start() {
        System.out.println("Start listening on port " +
                serverSocket.getLocalPort() + "...");

        try {
            while(true) {
                // accepts connections on the socket
                // After a client does connect, the ServerSocket creates a new Socket
                Socket client = serverSocket.accept();
                ClientThread t = new ClientThread(client);
                t.start();
                //clientThreadList.add(t);
            }
        } catch(SocketTimeoutException s) {
            System.out.println("Server timed out!");
        } catch(IOException e) {
            e.printStackTrace();
        }

    }

    private void close() {
        for(int i = loginList.clientThreadList.size() - 1; i>=0; i--) {
            loginList.clientThreadList.get(i).close();
        }
        try {
            serverSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    /* * * * * * * * * * * *
     * ClientThread - begin
     * * * * * * * * * * * */

    class ClientThread extends Thread{

        Socket socket;
        String name;//, user = null;
        DataInputStream in;
        DataOutputStream out;
        int maxAttempt = 3;

        public ClientThread(Socket client) {
            socket = client;
            name = "Client<" + socket.getRemoteSocketAddress() + "> ";
            System.out.println("Just connected to "
                    + socket.getRemoteSocketAddress());

            try {
                in = new DataInputStream(client.getInputStream());
                out = new DataOutputStream(client.getOutputStream());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void run(){
            try{
                socket.setSoTimeout(60000);//1min

                out.writeUTF("connected to server " + socket.getLocalSocketAddress() + "\n");

                // it may logout (close connection) inside following procedure
                // with IOException thrown
                if (authenticate()){
                    exec();
                }

                close();
            } catch (SocketTimeoutException e) {
                System.out.println("ClientThread timed out");
            } catch(IOException e){
                e.printStackTrace();
            }
        }

        private boolean authenticate() throws IOException {
            String username, psw;

            // verify username and get its saved psw
            out.writeUTF("Username: ");
            while (true) {
                username = in.readUTF();
                if (loginList.userList.contains(username)){
                    out.writeUTF(String.format("%s already login.\nUsername: ", username));
                } else {
                    psw = authenticator.getpsw(username);
                    if (psw == null) {
                        out.writeUTF("The user name does not exist.\nUsername: ");
                    }else break; //psw get
                }
            }

            // verify password
            int attemptLeft = maxAttempt;
            out.writeUTF("Password: ");
            while (attemptLeft > 0){
                attemptLeft--;
                if (authenticator.verify(psw, in.readUTF()) ){
                    out.writeUTF("Welcome to the simple chat server!\n");
                    setUser(username);
                    return true;
                } else{
                    if (attemptLeft == 0){
                        out.writeUTF(String.format(
                                "Wrong password. %d attempts failed. Bye bye~ ", maxAttempt));
                        // please try after blocktime
                        break;
                    }
                    out.writeUTF("Wrong password. Please try again.\nPassword: ");
                }
            }

            return false;
        }

        private void setUser(String username){
            String time = sdf.format(new Date());
            System.out.println(name + "(" + time + ") successfully login as " + username);
            //user = username; //~~~~~~~
            loginList.put(this, username);//"this" is ClientThread
        }

        private void logout(){
            loginList.remove(this);
            close();
        }

        private void exec() throws IOException {
            String cmd;
            while ((cmd = in.readUTF()) != null) {
                execCmd(cmd);

                //regexp??

                //if (outputLine.equals("Bye."))   break;
            }

        }

        private void execCmd(String command) throws IOException {
            System.out.println(name + command);

            // parse cmd
            String cmd;
            int cmdInd = command.indexOf(" ");
            if (cmdInd == -1){
                cmd = command;
            } else {
                cmd = command.substring(0, cmdInd);
            }
            Scanner s;
                //ref: [Scanner (Java Platform SE 7 )](http://docs.oracle.com/javase/7/docs/api/java/util/Scanner.html)


            if (cmd.equalsIgnoreCase("who")) {
                String output = String.join(" ", loginList.userList);
                out.writeUTF(output + "\n");
            } else if (cmd.equalsIgnoreCase("last")) {
                //s = new Scanner(command).useDelimiter("\\s*");
            } else if (cmd.equalsIgnoreCase("logout")) {
                logout();
            } else {
                out.writeUTF("Command cannot be recognized\n");
            }
        }

        private void close(){
            try {
                out.close();
                in.close();
                socket.close();
            }catch(Exception e) {}
        }

    }

    /* * * * * * * * * * * *
     * ClientThread - end
     * * * * * * * * * * * */

}
