import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class Server{

    // client timeout: 30min
    int time_out = 30, TIME_OUT = 60000 * time_out;
    // IP/username block time: 60s
    int block_time = 60, BLOCK_TIME = 1000 * block_time;

    static int SERVER_TIME_OUT = 3600000;//1h
    static int maxMinute = 60;// =>last
    static int maxAttempt = 3;// wrong password attempts

    static SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
    private ServerSocket serverSocket;
    private Authenticator authenticator;
    UserTracker userTracker;

    /* * * * * * * * * * * *
     * UserTracker - begin
     * * * * * * * * * * * */
    class UserTracker {
        // since there are not many users, list is sufficient

        ArrayList<String> onlineList;
        ArrayList<ClientThread> clientThreadList;
        ArrayDeque<Long> logoutTimeList;
        ArrayDeque<String> offlineList;

        HashMap<String, Long> userIPBlock;

        UserTracker(){
            onlineList = new ArrayList<String>();
            clientThreadList = new ArrayList<ClientThread>();

            userIPBlock = new HashMap<String, Long>();

            // in the order of logout time
            offlineList = new ArrayDeque<String>();
            logoutTimeList = new ArrayDeque<Long>();
        }

        public synchronized void put(ClientThread clientThread, String user){
            onlineList.add(user);// == clientThread.user
            clientThreadList.add(clientThread);
                // there is a mini bug. and I leave it alone.
                // if a user logs out and in, it will be in both on/offlineList.
                // I think it's better to restructure offlineList/logoutTimeList
                //  as list of pair. remove duplicates in reply to =>last
                //  is also plausible
        }

        public synchronized boolean remove(ClientThread clientThread){
            int ind = clientThreadList.indexOf(clientThread);
            if (ind == -1){
                return false;
            }
            remove(ind);
            return true;
        }
        public synchronized void remove(int ind){
            // log out
            logoutTimeList.add(System.currentTimeMillis());
            clientThreadList.remove(ind);
            offlineList.add(onlineList.remove(ind));
        }

        public ClientThread getClientThread(String user){
            return clientThreadList.get(onlineList.indexOf(user));
        }

        public synchronized void deleteObsolete(long time){
            while (logoutTimeList.size() > 0){
                if (logoutTimeList.getFirst() < time){
                    logoutTimeList.removeFirst();
                    offlineList.removeFirst();
                } else break;
            }
        }

        public Iterator getLastOnlineList(long time){
            Iterator i = offlineList.iterator(),
                    j = logoutTimeList.iterator();
            while (j.hasNext()){
                if ((Long) j.next() >= time) break;
                i.next();
            }
            return i;
        }

    }
    /* * * * * * * * * * * *
     * UserTracker - end
     * * * * * * * * * * * */


    public static void main(String [] args) {

        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                System.out.println("\nBye bye~");
            }
        });

        int port = Integer.parseInt(args[0]);
        Server server = new Server(port);
        server.start();
    }

    public Server(int port) {

        try {
            // create a server socket that listens on a port
            serverSocket = new ServerSocket(port);
            serverSocket.setSoTimeout(SERVER_TIME_OUT);//10min
        } catch (IOException e) {
            System.err.println("Could not listen on port " + port);
            System.exit(-1);
        }

        // read TIME_OUT from environment
        try {
            time_out = Integer.parseInt(System.getenv("TIME_OUT"));//min
            if (time_out > 0) TIME_OUT = 60000 * time_out;
        } catch (Exception e){}
        // read BLOCK_TIME from environment
        try{
            block_time = Integer.parseInt(System.getenv("BLOCK_TIME"));//s
            if (block_time > 0) BLOCK_TIME = 1000 * block_time;
        } catch (Exception e){}
        System.out.println("Client TIME_OUT is " + time_out + " minute(s).");
        System.out.println("Username/IP BLOCK_TIME is " + time_out + " second(s).");

        authenticator = new Authenticator("user_pass.txt");
        userTracker = new UserTracker();
    }

    public void start() {
        System.out.println("Start listening on port " +
                serverSocket.getLocalPort() + "...");

        try {
            while(true) {
                // accepts connections on the socket
                Socket client = serverSocket.accept();
                ClientThread t = new ClientThread(client);
                t.start();
            }
        } catch(SocketTimeoutException s) {
            System.out.println("Server timed out!");
        } catch(IOException e) {
            e.printStackTrace();
        }
        close();
    }

    /*
     * close server socket and client threads
     */
    private void close() {
        System.out.println("Start closing all client threads ...");
        ClientThread clientThread;
        for(int i = userTracker.clientThreadList.size() - 1; i>=0; i--) {
            clientThread = userTracker.clientThreadList.get(i);
            System.out.println("\t" + clientThread.name + "closed");
            clientThread.close();
        }
        try {
            serverSocket.close();
            System.out.println("Server socket closed");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    /* * * * * * * * * * * *
     * ClientThread - begin
     * * * * * * * * * * * */
    // the list of commands the server supports
    static List<String> zeroArgCmd = Arrays.asList("who", "logout");
    static List<String> oneArgCmd = Arrays.asList("last", "broadcast");
    static List<String> twoArgCmd = Collections.singletonList("send");
    // patterns for parsing commands
    static Pattern patternGetCmd = Pattern.compile("\\s*(\\w+)\\s*(.*)\\s*"); // delete \s in the beginning and end
    static Pattern patternGetTwoArgs = Pattern.compile("[(](.+)[)]\\s+(.+)|(.+)\\s+[(](.+)[)]|(\\w+)\\s+(.+)");
    /*
     *
     */
    class ClientThread extends Thread{

        Socket socket;
        String name, user, ip;
        DataInputStream in;
        DataOutputStream out;
        int state = 0;

        public ClientThread(Socket client) {
            socket = client;
            ip = socket.getInetAddress().getHostAddress();
                // socket.getRemoteSocketAddress()
            name = ip + " ";
            System.out.println("Just connected to " + ip);

            try {
                in = new DataInputStream(client.getInputStream());
                out = new DataOutputStream(client.getOutputStream());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void run(){
            try{
                socket.setSoTimeout(TIME_OUT);

                out.writeUTF("connected to server " + socket.getLocalSocketAddress() + "\n");

                // it may logout (close connection) inside following procedure
                // with IOException thrown
                String cmd;
                if (authenticate()){
                    while ((cmd = in.readUTF()) != null) {
                        execCmd(cmd);
                    }
                } else{
                    System.out.println(name + "did not pass authentication. ");
                    close();
                }

            } catch (SocketTimeoutException e) {
                System.out.println(name + "timed out. ");
                try {
                    out.writeUTF("\nTimed out. ");
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
                logout();
            } catch(IOException e){
                if (state != 0){
                    logout();
                } else e.printStackTrace();
            }

        }

        private boolean authenticate() throws IOException {
            String username, psw;

            // verify username and get its saved psw
            out.writeUTF("Username: ");
            while (true) {
                username = in.readUTF();
                if (userTracker.onlineList.contains(username)){
                    out.writeUTF(String.format("%s already login.\nUsername: ", username));
                } else {
                    psw = authenticator.getpsw(username);
                    if (psw == null) {
                        out.writeUTF("Username does not exist.\nUsername: ");
                    }else break; //psw get
                }
            }

            String userip = username + ":" + ip;
            try {
                if (userTracker.userIPBlock.get(userip) >= System.currentTimeMillis()) {
                    out.writeUTF("Your IP is blocked to login as " + username);
                    return false;
                } else{
                    userTracker.userIPBlock.remove(userip);
                }
            } catch (Exception e){}

            // verify password
            int attemptLeft = maxAttempt;
            out.writeUTF("Password: ");
            while (attemptLeft > 0){
                attemptLeft--;
                // Password "" is also considered as an attempt
                if (authenticator.verify(psw, in.readUTF()) ){
                    out.writeUTF("Welcome to the simple chat server!\n");
                    setUser(username);
                    return true;
                } else{
                    if (attemptLeft == 0){
                        out.writeUTF(String.format("Wrong password. \n%d attempts failed. " +
                                "Please try after %d seconds to login as %s.",
                                maxAttempt, block_time, username));
                        userTracker.userIPBlock.put(userip, System.currentTimeMillis() + BLOCK_TIME);
                        break;
                    }
                    out.writeUTF("Wrong password. Please try again.\nPassword: ");
                }
            }

            return false;
        }

        public long getTimeBeforeMinute(int minute){
            long millis = System.currentTimeMillis();
            return millis - minute * 60000;
        }

        private void setUser(String username){
            Date date = new Date();
            String time = sdf.format(date);
            name = "[" + username + "](" + ip + ") ";
            System.out.println(name + "successfully login at " + time);
            state++;// login
            user = username;
            userTracker.put(this, username);//"this" is ClientThread
        }

        private void logout(){
            state--;// logout
            close();
            if (userTracker.remove(this)){
                System.out.println(name + "successfully logged out with state = " + state);
                // no need to reply to client
            } else {
                System.out.println(name + "state = " + state);
            }
        }

        private String replyToCmdWho() throws IOException {
            //out.writeUTF(String.join(" ", userTracker.onlineList));
                //seems not supported in java 7-
            String reply = "";
            for (String username : userTracker.onlineList){
                reply += username + " ";
            }
            return reply;
        }

        private void execCmd(String command) throws IOException {
            System.out.println(name + "=> " + command);

            // parse cmd
            String cmd = "", arg = "";
            try {
                Matcher matcher = patternGetCmd.matcher(command);
                matcher.matches();
                cmd = matcher.group(1);
                arg = matcher.group(2);
            } catch (Exception e) {}

            // execute command according to their parameters
            if (cmd.matches("\\s*")){
                // do nothing
                out.writeUTF("");
            } else if (zeroArgCmd.contains(cmd)){
                if (arg.equals("")){
                    if (cmd.equals("who")) {
                        out.writeUTF(replyToCmdWho());
                    } else if (cmd.equals("logout")){ // => logout
                        logout();
                    }
                } else{
                    out.writeUTF("Usage: " + cmd);
                }

            } else if (oneArgCmd.contains(cmd)){
                if (cmd.equals("broadcast")){
                    if (arg.equals("")){
                        out.writeUTF("Usage: broadcast <message>");
                    } else{
                        System.out.println(name + "broadcast: " + arg);
                        broadcast(arg);
                    }
                } else { // => last
                    try{
                        int minute = Integer.parseInt(arg);
                        String reply = "";
                        if (minute > 60 || minute < 0){
                            reply = "Please input minute between 0 and 60.";
                        } else {
                            long time0 = getTimeBeforeMinute(maxMinute), time = getTimeBeforeMinute(minute);
                            userTracker.deleteObsolete(time0);
                            Iterator i = userTracker.getLastOnlineList(time);
                            while (i.hasNext()){
                                reply += i.next() + " ";
                            }
                            reply += replyToCmdWho();
                        }
                        out.writeUTF(reply);
                    } catch (NumberFormatException e){
                        out.writeUTF("Usage: last <number>");
                    } catch (Exception e){
                        e.printStackTrace();
                    }
                }

            } else if (twoArgCmd.contains(cmd)){ // => send
                String[] recUsers = null;
                String message = null;
                Matcher matcher = patternGetTwoArgs.matcher(arg);
                if (matcher.matches()){
                    if (matcher.group(1) != null) {
                        recUsers = matcher.group(1).split("\\s+");
                        message = matcher.group(2);
                    } else if (matcher.group(3) != null){
                        recUsers = matcher.group(3).split("\\s+");
                        message = matcher.group(4);
                    } else if (matcher.group(5) != null){
                        recUsers = new String[]{matcher.group(5)};
                        message = matcher.group(6);
                    }
                    sendMessage(recUsers, message);
                } else{
                    out.writeUTF("Usage: send (<user> ... <user>) <message> \n" +
                            "  or: send <user> <message>");
                }

            } else {
                out.writeUTF("Command not found.");
            }

        }

        private void sendMessage(String[] recUsers, String msg){
            ClientThread clientThread;
            String message = "\n[" + user + "] " + msg;
            String mymessage = "";
            int count = 0;
            for (String recUser : recUsers){
                try{
                    clientThread = userTracker.getClientThread(recUser);
                    if (clientThread == this){
                        mymessage = "[YOU] " + msg + "\n";
                        count++;
                        continue;
                    }
                    clientThread.out.writeUTF(message);
                    count++;
                } catch (Exception e) {
                    System.out.println("Failed to send message to " + recUser);
                }
            }
            try {
                out.writeUTF(mymessage + "Your message is successfully send to " + count + " online users.");
            } catch (IOException e) {}

        }

        private void broadcast(String msg) { //~~ what happened when multiple clients broadcast? synchronized?
            String message = "\n[" + user + "] " + msg;
                // start with "\n" so that it will not print right after "=> "
            int count = 0;
            for (ClientThread clientThread : userTracker.clientThreadList ){
                if (clientThread == this) continue; // do not broadcast msg to oneself
                try {
                    clientThread.out.writeUTF(message);
                    count++;
                } catch (IOException e) {
                    System.out.println("Failed to broadcast message to " + clientThread.name);
                    e.printStackTrace();
                }
            }
            try {
                out.writeUTF("Your message is successfully broadcast to " + count + " online users.");
            } catch (IOException e) {
                e.printStackTrace();
            }

        }

        private void close(){
            try {
                out.close();
                in.close();
                socket.close();
                System.out.println(name + "closed.");
            }catch(Exception e) {}
        }

    }
    /* * * * * * * * * * * *
     * ClientThread - end
     * * * * * * * * * * * */

}
