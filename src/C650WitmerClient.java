import java.io.IOException;
import java.io.PrintWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.util.Random;

public class C650WitmerClient{
    public static void main(String[] args) throws IOException {
        //make the TCP connection
        Socket s = new Socket("localhost", 2111);
        
        //send the message along with the port number as is expected by the server
        PrintWriter pr = new PrintWriter(s.getOutputStream()); 
        pr.println("Hello");
        pr.flush(); 
        int p = getPort(); 
        pr.println(p); 
        pr.flush(); 
        
        //close the resources
        s.close();
        pr.close();

        //now that we have made the connection with the server, establish a UDP connection and receive the packets
        byte[][] contents = getUDPFile(p); 

    }

    //private methods...
    private static int getPort(){
        Random r = new Random(); 
        Integer i = r.nextInt() % 65536;
        if(i >= 16384)
            return i; 
        else
            return getPort();   
    }

    private static byte[][] getUDPFile(int p) throws SocketException, IOException{
        byte[] receive = new byte[100]; 
        DatagramSocket ds = new DatagramSocket(p); 
        DatagramPacket dp = new DatagramPacket(receive, receive.length);  
        //get the first packet
        ds.receive(dp);
        //get the port to send the ack back on
        int packetPort = dp.getPort(); 
        //number of packets that will be received
        int numPackets = Character.getNumericValue((char)receive[1]);
        
        //create a 2d array to store the incoming packets based off of the packet header
        byte[][] contents = new byte[numPackets][receive.length];
        
        //store the bytes from the first packet received 
        int currentPacket = Character.getNumericValue((char)receive[0]); 
        for(int i = 0; i < receive.length-2; i++){
            contents[currentPacket][i] = receive[i+2]; 
        }

        boolean[] hasCompleteFile = new boolean[numPackets];
        hasCompleteFile[currentPacket] = true; 

        //fill the rest of the 2d array with subsequent packets
        for(int j = 1 ; j < numPackets; j++){
            ds.receive(dp);
            currentPacket = Character.getNumericValue((char)receive[0]);  
            hasCompleteFile[currentPacket] = true; 
            for(int i = 0; i < receive.length-2; i++){
                contents[currentPacket][i] = receive[i+2]; 
            }
        }

        //return the contents if the packet is complete
        for(boolean b : hasCompleteFile){
            if(!b)
                return null; 
        }
        ds.close();
        sendAck(packetPort);
        return contents; 
    }

    private static void sendAck(int p) throws SocketException, IOException{
        DatagramSocket ds = new DatagramSocket();
        String msg = "ACK"; 
        byte[] ackMsg = msg.getBytes(); 
        DatagramPacket dp = new DatagramPacket(ackMsg, ackMsg.length, InetAddress.getLocalHost(), p); 
        ds.send(dp);
        ds.close();
        System.out.println(p);
    }


}

