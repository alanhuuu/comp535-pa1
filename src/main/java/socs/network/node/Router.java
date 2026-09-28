package socs.network.node;

import socs.network.message.SOSPFPacket;
import socs.network.util.Configuration;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;


public class Router {

  protected LinkStateDatabase lsd;

  RouterDescription rd = new RouterDescription();

  // Create a shared BufferedReader for reading user input from the console
  private final BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
  private volatile boolean attachRequestPending = false;
  private volatile String attachResponse = null;

  //assuming that all routers are with 4 ports
  Link[] ports = new Link[4];
  private ServerSocket serverSocket;

  public Router(Configuration config) {
    try{
      rd.simulatedIPAddress = config.getString("socs.network.router.ip");

      serverSocket = new ServerSocket(0);
      rd.processIPAddress = InetAddress.getLocalHost().getHostAddress();
      rd.processPortNumber = serverSocket.getLocalPort();

      lsd = new LinkStateDatabase(rd);

      // The "serverSocket.accept()" method is a blocking call, so we need to run it in a separate thread to avoid blocking the main thread
      Thread listenerThread  = new Thread(this::listener);
      listenerThread.start();

    } catch (Exception e) {
      throw new RuntimeException("Failed to intialize router", e);
    }
  }

  /**
   * output the shortest path to the given destination ip
   * <p/>
   * format: source ip address  -> ip address -> ... -> destination ip
   *
   * @param destinationIP the ip adderss of the destination simulated router
   */
  private void processDetect(String destinationIP) {

  }

  /**
   * disconnect with the router identified by the given destination ip address
   * Notice: this command should trigger the synchronization of database
   *
   * @param portNumber the port number which the link attaches at
   */
  private void processDisconnect(short portNumber) {

  }

  /**
   * attach the link to the remote router, which is identified by the given simulated ip;
   * to establish the connection via socket, you need to indentify the process IP and process Port;
   * additionally, weight is the cost to transmitting data through the link
   * <p/>
   * NOTE: this command should not trigger link database synchronization
   */
  private void processAttach(String processIP, int processPort, String simulatedIP, short weight) {
    try{
      // Finding an empty port to attach the link
      int freePortIdx = -1;

      for (int i = 0; i < ports.length; i++) {
        if (ports[i] == null) {
          freePortIdx = i;
          break;
        }
      }

      if (freePortIdx == -1) {
        System.out.println("No available ports.");
        return;
      }
      
      Socket socket = new Socket(processIP, processPort);

      // Build "attach" request message
      SOSPFPacket hello = new SOSPFPacket();

      hello.srcProcessIP = rd.processIPAddress;
      hello.srcProcessPort = rd.processPortNumber;
      hello.srcIP = rd.simulatedIPAddress;
      hello.dstIP = simulatedIP;
      hello.sospfType = 0;
      hello.neighborID = rd.simulatedIPAddress;
      hello.weight = weight;
      
      ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
      out.flush();
      ObjectInputStream in = new ObjectInputStream(socket.getInputStream());

      out.writeObject(hello);
      out.flush();

      boolean accepted = in.readBoolean();

      if(!accepted) {
        System.out.println("Your attach request has been rejected;");
        socket.close();
        return;
      }

      RouterDescription remoteRouter = new RouterDescription();

      remoteRouter.processIPAddress = processIP;
      remoteRouter.processPortNumber = processPort;
      remoteRouter.simulatedIPAddress = simulatedIP;

      ports[freePortIdx] = new Link(rd, remoteRouter);

      System.out.println("Your attach request has been accepted;");

      socket.close();

    }
    catch (Exception e){
      System.out.println("Failed to connect to the remote router. Please check the process IP and process port.");
    }
    
  }

  private void listener(){
    while(true){
      try{
        Socket socket = serverSocket.accept();

        requestHandler(socket);
      }
      catch (Exception e){
        System.out.println("Failed to accept the connection from the remote router.");
      }
    }
  }

  /**
   * process request from the remote router. 
   * For example: when router2 tries to attach router1. Router1 can decide whether it will accept this request. 
   * The intuition is that if router2 is an unknown/anomaly router, it is always safe to reject the attached request from router2.
   */
  private void requestHandler(Socket socket) {
    try{
      ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
      out.flush();
      ObjectInputStream in = new ObjectInputStream(socket.getInputStream());

      SOSPFPacket request = (SOSPFPacket) in.readObject();

      if (request.sospfType != 0){
        socket.close();
        return;
      }

      int freePortIdx = -1;

      for (int i = 0; i < ports.length; i++) {
        if (ports[i] == null) {
          freePortIdx = i;
          break;
        }
      }

      if (freePortIdx == -1) {
        out.writeBoolean(false);
        out.flush();

        System.out.println("No available ports.");
        socket.close();
        return;
      }

      System.out.println();
      System.out.println("received HELLO from " + request.srcIP + ";");
      System.out.println();
      System.out.println("Do you accept this request? (Y/N)");

      attachRequestPending = true;
      while (attachResponse == null) {
        Thread.sleep(10);
      }
      String response = attachResponse;
      attachResponse = null;

      if (response.equalsIgnoreCase("Y")) {
        RouterDescription remoteRouter = new RouterDescription();
        remoteRouter.processIPAddress = request.srcProcessIP;
        remoteRouter.processPortNumber = request.srcProcessPort;
        remoteRouter.simulatedIPAddress = request.srcIP;

        ports[freePortIdx] = new Link(rd, remoteRouter);

        out.writeBoolean(true);
        out.flush();

        System.out.println("You have accepted the attach request from " + request.srcIP);
        socket.close();
      } 
      else {
        out.writeBoolean(false);
        out.flush();
        System.out.println("You have rejected the attach request from " + request.srcIP);
        socket.close();
        return;
      }
    }
    catch (Exception e){
      System.out.println("Failed to process the request from the remote router.");
    }
    finally {
      attachRequestPending = false;
    }
  }

  /**
   * broadcast Hello to neighbors
   */
  private void processStart() {

  }

  /**
   * attach the link to the remote router, which is identified by the given simulated ip;
   * to establish the connection via socket, you need to indentify the process IP and process Port;
   * additionally, weight is the cost to transmitting data through the link
   * <p/>
   * This command does trigger the link database synchronization
   */
  private void processConnect(String processIP, int processPort,
                              String simulatedIP, short weight) {

  }

  /**
   * output the neighbors of the routers
   */
  private void processNeighbors() {

  }

  /**
   * disconnect with all neighbors and quit the program
   */
  private void processQuit() {

  }

  /**
   * update the weight of an attached link
   */
  private void updateWeight(String processIP, int processPort,
                             String simulatedIP, short weight){

  }

  /**
   * update the weight of a specific port.
   * This change should trigger synchronization of the Link State Database by sending 
   * a Link State Advertisement (LSA) update to all neighboring routers in the topology.
   *
   * @param portNumber the port number (0-3) to update
   * @param newWeight the new weight/cost for the link attached to this port
   */
  private void processUpdate(short portNumber, short newWeight) {

  }

  /**
   * send an application-level message from this router to the destination router.
   * The message must be forwarded hop-by-hop according to the current shortest path.
   * <p/>
   * When you run send, the window of the router where you run the command should print:
   * "Sending message to <Destination IP>"
   * <p/>
   * For each intermediate router on the shortest path (excluding the source and destination), 
   * the router window should print:
   * "Forwarding packet from <Source IP> to <Destination IP>"
   * <p/>
   * When the destination router receives the message, the router window should print:
   * "Received message from <Source IP>:"
   * "<Message>"
   *
   * @param destinationIP the simulated IP address of the destination router
   * @param message the message content to send
   */
  private void processSend(String destinationIP, String message) {

  }

  /**
   * handle incoming application message packet.
   * This method should be called when a router receives a SOSPFPacket with sospfType = 2 (Application Message).
   * <p/>
   * If this router is the destination (packet.dstIP equals this router's simulatedIPAddress):
   * - Print "Received message from <Source IP>:"
   * - Print the message content
   * <p/>
   * If this router is an intermediate router:
   * - Print "Forwarding packet from <Source IP> to <Destination IP>"
   * - Forward the packet to the next hop on the shortest path to the destination
   * - Do NOT print or inspect the message payload
   *
   * @param packet the received application message packet
   */
  private void handleApplicationMessage(socs.network.message.SOSPFPacket packet) {

  }

  public void terminal() {
    System.out.println("========================================");
    System.out.println();
    System.out.println("Process IP      : " + rd.processIPAddress);
    System.out.println();
    System.out.println("Process Port    : " + rd.processPortNumber);
    System.out.println();
    System.out.println("Simulated IP    : " + rd.simulatedIPAddress);
    System.out.println();
    System.out.println("========================================");

    try {
      while (true) {
        System.out.print(">> ");
        String command = br.readLine();
        if (attachRequestPending) {
          attachResponse = command;
          while (attachRequestPending) {
            Thread.sleep(10);
          }
          continue;
        }

        if (command.startsWith("detect ")) {
          String[] cmdLine = command.split(" ");
          processDetect(cmdLine[1]);
        } else if (command.startsWith("disconnect ")) {
          String[] cmdLine = command.split(" ");
          processDisconnect(Short.parseShort(cmdLine[1]));
        } else if (command.startsWith("quit")) {
          processQuit();
        } else if (command.startsWith("attach ")) {
          String[] cmdLine = command.split(" ");
          processAttach(cmdLine[1], Integer.parseInt(cmdLine[2]),
                  cmdLine[3], Short.parseShort(cmdLine[4]));
        } else if (command.equals("start")) {
          processStart();
        } else if (command.startsWith("connect ")) {
          String[] cmdLine = command.split(" ");
          processConnect(cmdLine[1], Integer.parseInt(cmdLine[2]),
                  cmdLine[3], Short.parseShort(cmdLine[4]));
        } else if (command.equals("neighbors")) {
          //output neighbors
          processNeighbors();
        } else if (command.startsWith("send ")) {
          //send [Destination IP] [Message]
          String[] cmdLine = command.split(" ", 3);
          if (cmdLine.length >= 3) {
            processSend(cmdLine[1], cmdLine[2]);
          } else {
            System.out.println("Usage: send [Destination IP] [Message]");
          }
        } else if (command.startsWith("update ")) {
          //update [port_number] [new_weight]
          String[] cmdLine = command.split(" ");
          if (cmdLine.length >= 3) {
            processUpdate(Short.parseShort(cmdLine[1]), Short.parseShort(cmdLine[2]));
          } else {
            System.out.println("Usage: update [port_number] [new_weight]");
          }
        } else {
          //invalid command
          break;
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

}
