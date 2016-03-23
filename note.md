[Java and Makefiles](https://www.cs.swarthmore.edu/~newhall/unixhelp/javamakefiles.html)


`mvn clean install`

build.xml


#### todo

search for ~~

want it to exit instantly after log out without enter, so does for timeout. 
but seems that read will block everything.
                
check when client close connection, how is loginList, 
i think i should carefully close those lost clientThreads
 check the logistics of all try/catch, that's where you close them

also client side, think about when it will be closed

    //private void close(){}
getpeername()
spin lock
ex: KeyboardInterrupt

test pass

utf-8
 InputStream is = process.getInputStream();
                InputStreamReader isr = new InputStreamReader(is, "UTF-8");
                BufferedReader br = new BufferedReader(isr);

not really extra feature. but make the terminal robust to no command / invalid command arguments 

1. [Java Makefile Examples](http://jwrr.com/content/Gnu-Makefile-Examples/)


#### Reference

1. [Java - Networking](http://www.tutorialspoint.com/java/java_networking.htm)
2. [A Simple Chat Program With Client/Server (GUI Optional) - Java Tutorials | Dream.In.Code](http://www.dreamincode.net/forums/topic/259777-a-simple-chat-program-with-clientserver-gui-optional/)
   ObjectInputStream/ObjectOutputStream
1. [Maven – Users Centre](https://maven.apache.org/users/index.html)
    [Maven – Introduction to the Standard Directory Layout](https://maven.apache.org/guides/introduction/introduction-to-the-standard-directory-layout.html)
3. [Writing the Server Side of a Socket (The Java™ Tutorials > Custom Networking > All About Sockets)](http://docs.oracle.com/javase/tutorial/networking/sockets/clientServer.html)
   PrintWriter??
   


1. [swing - Very Basic terminal in Java - Stack Overflow](http://stackoverflow.com/questions/7670132/very-basic-terminal-in-java)

  Code snippet

```
Thread input = new Thread() {
    @Override public void run() {
      byte[] buffer = new byte[1024];
      while(!shellSession.isClosed()) {
        int read = System.in.read(buffer);
        shellSession.getInputStream().write(buffer, read);
      }
    }
  }
```


                ref: [Scanner (Java Platform SE 7 )](http://docs.oracle.com/javase/7/docs/api/java/util/Scanner.html)

Scanner s = new Scanner(command).useDelimiter("\\s*");


ThreadingTCPServer or ForkingTCPServer

https://docs.python.org/3/howto/sockets.html



#### note

/Server socket.getLocalSocketAddress(): /127.0.0.1:4119
/Client socket.getRemoteSocketAddress(): localhost/127.0.0.1:4119


#### postscript

This is somewhat my very first Java project. 


#### my question upon Java

1. when put one class in another class
2. anything like: in = input("> ")
2. overwrite a built-in method
3. without private/public, what category it is then?
4. learn more about synchronized
5. create object inside/outside constructor. 
    <http://stackoverflow.com/questions/9282706/instantiating-object-in-a-constructor>

Socket(InetAddress.getByAddress(addr), port);
