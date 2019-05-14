import java.io.File;
import java.io.FileNotFoundException;
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
        DatagramSocket ds = new DatagramSocket(p);
        pr.println(p); 
        pr.flush(); 
        
        //close the resources
        s.close();
        pr.close();

        //now that we have made the connection with the server, establish a UDP connection and receive the packets
        byte[][] contents = null;  
        
        //if getUDPFile returns null, wait again. getUDPFile returns null if none or only some of the packets were received.
        while(contents == null){
            contents = getUDPFile(ds, p);
        }
        //write the contents of the data sent to the file
        String message = getStringFromMessage(contents); 
        
        //this was written and tested on a mac. line 41 was edited just before submission to work with windows
        writeFile("c:/c650projs19/ctestfile", message); 
        
        //if get message "OK", close the UPD socket. 
        contents = getUDPFile(ds, p); 
        message = getStringFromMessage(contents); 
        if(message.contains("OK")){
            System.out.println("Done");
            System.out.println(p);
            ds.close();
        }
            
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

    private static byte[][] getUDPFile(DatagramSocket ds, int p) throws SocketException, IOException{
        byte[] receive = new byte[100];  
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
        sendAck(ds, packetPort);
        return contents; 
    }

    private static void sendAck(DatagramSocket ds, int p) throws SocketException, IOException{
        String msg = "ACK"; 
        byte[] ackMsg = msg.getBytes(); 
        DatagramPacket dp = new DatagramPacket(ackMsg, ackMsg.length, InetAddress.getLocalHost(), p); 
        ds.send(dp);
        System.out.println(p);
        System.out.println("Ack Sent");
    }

    private static void writeFile(String location, String message) throws FileNotFoundException{
        PrintWriter out = new PrintWriter(location); 
        out.println(message); 
        out.close();
    }

    private static String getStringFromMessage(byte[][] contents){
        String message = ""; 
        for(byte[] B: contents){
            for (byte b: B){
                message += ((char)b);
            }
        }
        return message; 
    }

}

