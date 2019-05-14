import java.io.BufferedReader;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.sql.Time;
import java.util.Scanner;
import javax.activation.FileDataSource;
import java.io.FileInputStream;
import java.lang.Math;
import java.util.Date; 
 

public class C650WitmerServer{
    public static void main(String[] args) throws IOException {
        Scanner input = new Scanner(System.in);
        System.out.print("Enter an integer timeout T in ms (e.g., 100 means 100 ms): ");
        int t = input.nextInt();
        System.out.print("Enter the size M of all packets (except the last) in bytes (e.g., 1200 means 1200 bytes): ");
        int m = input.nextInt();
        input.close();
        
        //read in the file and make sure the given value of m is smaller
        //than the sum of all of the bytes in the array
        byte[] file = getFileArray(); 
        if(m > file.length){
            System.out.println("Your given size for m is larger than the file.");
            return; 
        }

       // serve the TCP connection and 
       ServerSocket ss = new ServerSocket(2111);
       Socket s; 
       InputStreamReader in;
       BufferedReader bf; 


       //as long as the server is open, keep listening for clients
       while(true){
            //listen for clients responding and get the port for UDP connection   
            s = ss.accept(); 
            int p; 
           in = new InputStreamReader(s.getInputStream()); 
           bf = new BufferedReader(in);
           
           //any client connecting on this program is expected to send "Hello" and its port
           if(bf.readLine().equals("Hello")){ 
               //get the port from the client
               p = Integer.parseInt(bf.readLine());   
               System.out.println("\nHello Received\nPort: " + p +'\n');
               s.close(); 

               //now that we have the port, we will establish the UPD connection and send the file 
               DatagramSocket ds = new DatagramSocket();  
               sendFile(ds,file, m, p); 

               //await the ack and send again if no ack comes
               int timeout = t; 
               ds.setSoTimeout(timeout);
               while(true){
                    try{
                        if(awaitAck(ds, p, t) == true){
                            System.out.println("OK");
                            //send okay message back to client
                            byte[] okayMessage = "OK".getBytes(); 
                            sendFile(ds, okayMessage, m, p);
                            break;
                        } 
                    }catch(SocketTimeoutException e){
                        ds.setSoTimeout(timeout * 2);
                        System.out.println(p);
                        System.out.println("Resending");
                        sendFile(ds,file, m, p); 
                    }
               }

           }
                  
       }

    }

    //private methods
    private static byte[] getFileArray() throws IOException {
        //read file into byte array
        File file = new File("/Users/deronwitmer/c650projs19/stestfile.txt");
        byte[] bytesArray = new byte[(int) file.length()]; 
        FileInputStream fis = new FileInputStream(file);
        fis.read(bytesArray);
        fis.close();
        return bytesArray; 
    }

    private static void sendFile(DatagramSocket ds, byte[] file, int m, int p) throws SocketException, UnknownHostException, IOException{
        int numPackets = (int) Math.round(Math.ceil(file.length/new Double(m)));  
        //form the packet
        int byteNumber = 0; 
        int packetNumber=0; 
        while(true){
            byte[] contents = new byte[m+2]; 
            contents[0] = (byte)((char)(packetNumber++ +'0')); 
            contents[1] = (byte)((char)numPackets + '0'); 
            for(int i = 2; i < m+2; i++){
                if(byteNumber < file.length)
                    contents[i] = file[byteNumber++];  
            }

            //send the packet
            DatagramPacket dp = new DatagramPacket(contents, contents.length, InetAddress.getLocalHost(), p);
            ds.send(dp);
            if(byteNumber >= file.length){
                break;
            }
        }
    }

    private static boolean awaitAck(DatagramSocket ds, int p, int timeout) throws IOException, SocketException{  
        byte[] receive = new byte[1024]; 
        DatagramPacket dp = new DatagramPacket(receive, receive.length); 
        ds.receive(dp);
        String msg = ""; 
        for(byte b : receive){
            msg = msg + (char)b; 
        }
        if(msg.contains("ACK"))
            return true; 
        return false; 
    }
}