import java.io.*;
import java.util.*;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Paths;

public class peer {

    public static Dictionary peersPeer5 = new Hashtable();
    public static int peerNumber = 1;
    static int nChunks = 0;

    public static String getFiles() {
        File directory = new File(".");
        File[] filesList = directory.listFiles();
        String directoryString = "";
        for(File f: filesList) {
            if(f.getName().contains("part")) {
                directoryString += f.getName() + "\n";
            }
        }
        return(directoryString.substring(0, directoryString.length() - 1));
    }

    public static boolean hasAllFiles() {
        String files = getFiles();
        String[] filesSplit = files.split("\n");
        if(filesSplit.length == nChunks) {
            return true;
        }
        return false;
    }

    public static void getNumChunks() {
      String files;
      File directory = new File("../file_owner/");
      File[] filesList = directory.listFiles();
      String directoryString = "";
      for(File f: filesList) {
          if(f.getName().contains("part")) {
              //directoryString += f.getName() + "\n";
              String[] filesSplit = f.getName().split(".part");
              if(Integer.parseInt(filesSplit[1]) > nChunks) {
                nChunks = Integer.parseInt(filesSplit[1]);
              }
          }
      }
    }

    public static void mergeAllParts() {
        String FILE_NAME = "CLRS.pdf";
        File ofile = new File(FILE_NAME);
		FileOutputStream fos;
		FileInputStream fis;
		byte[] fileBytes;
		int bytesRead = 0;
		List<File> list = new ArrayList<File>();
        for(int i = 1; i <= nChunks; i ++) {
            list.add(new File(FILE_NAME + ".part" + i));
        }
		try {
		    fos = new FileOutputStream(ofile,true);
		    for (File file : list) {
		        fis = new FileInputStream(file);
		        fileBytes = new byte[(int) file.length()];
		        bytesRead = fis.read(fileBytes, 0,(int)  file.length());
		        assert(bytesRead == fileBytes.length);
		        assert(bytesRead == (int) file.length());
		        fos.write(fileBytes);
		        fos.flush();
		        fileBytes = null;
		        fis.close();
		        fis = null;
		    }
		    fos.close();
		    fos = null;
		}catch (Exception exception){
			exception.printStackTrace();
		}
    }

    public static void main(String[] args) throws IOException {
        ServerSocket serverSocket = new ServerSocket(7005);
        getNumChunks();
        try {
            InetAddress inetIp = InetAddress.getByName("localhost");
            Socket socket = new Socket(inetIp, 5056);
            DataInputStream dataInputStream = new DataInputStream(socket.getInputStream());
            DataOutputStream dataOutputStream = new DataOutputStream(socket.getOutputStream());
            Scanner sc = new Scanner(System.in);
            while(true) {
                System.out.println(dataInputStream.readUTF());
                String toSend = sc.nextLine();
                dataOutputStream.writeUTF(toSend);
                if (toSend.equals("Exit")) {
                    System.out.println("Closing this connection.");
                    socket.close();
                    System.out.println("Connection Closed.");
                    break;
                }
                //String recieved = dataInputStream.readUTF();
                //System.out.println(recieved);
            }
            //sc.close();
            dataInputStream.close();
            dataOutputStream.close();
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        Thread thread1 = new Thread () {
            public void run () {
                try {
                    Socket socketPeer5Client = new Socket(InetAddress.getByName("localhost"), 7004);
                    DataInputStream dataInputStreamPeer5Client = new DataInputStream(socketPeer5Client.getInputStream());
                    DataOutputStream dataOutputStreamPeer5Client = new DataOutputStream(socketPeer5Client.getOutputStream());
                    while(true) {
                        String clientFiles = getFiles();
                        String serverFiles = dataInputStreamPeer5Client.readUTF();
                        String[] serverFileList = serverFiles.split("\n");
                        if(hasAllFiles()) {
                            dataOutputStreamPeer5Client.writeUTF("Exit");
                            System.out.println("Receieved all parts.\nClosing this connection.");
                            mergeAllParts();
                            socketPeer5Client.close();
                            System.out.println("Connection Closed.");
                            break;
                        }
                        for(String fileName: serverFileList) {
                            if(!clientFiles.contains(fileName)) {
                                System.out.println("Requesting for file " + fileName + " from Peer4");
                                dataOutputStreamPeer5Client.writeUTF("Sending file " + fileName + " to Peer5");
                                String source = "../peer4/" + fileName;
                                File destination = new File(fileName);
                                Files.copy(Paths.get(source), destination.toPath());
                                break;
                            }
                        }
                        System.out.println(dataInputStreamPeer5Client.readUTF());
                    }
                    dataInputStreamPeer5Client.close();
                    dataOutputStreamPeer5Client.close();
                }
                catch(Exception e) {
                    e.printStackTrace();
                }
            }
        };
        Thread thread2 = new Thread () {
            public void run() {
                try {
                    //ServerSocket serverSocket = new ServerSocket(7003);
                    while(true) {
                        Socket socketPeer5Server = serverSocket.accept();
                        System.out.println("\nA new peer was connected.");
                        DataInputStream dataInputStreamPeer5Server = new DataInputStream(socketPeer5Server.getInputStream());
                        DataOutputStream dataOutputStreamPeer5Server = new DataOutputStream(socketPeer5Server.getOutputStream());
                        System.out.println("A new thread was allocated for this peer.");
                        peer.peersPeer5.put(socketPeer5Server.getPort(), "peer" + peerNumber);
                        peerNumber++;
                        Thread thread = new PeerHandler(socketPeer5Server, dataInputStreamPeer5Server, dataOutputStreamPeer5Server);
                        thread.start();
                    }
                }
                catch(Exception e) {
                    e.printStackTrace();
                }
            }
        };
        thread1.start();
        thread2.start();
    }

}

class PeerHandler extends Thread {

    DataInputStream dataInputStream;
    DataOutputStream dataOutputStream;
    Socket socket;
    Scanner sc = new Scanner(System.in);

    public PeerHandler(Socket socket, DataInputStream dataInputStream, DataOutputStream dataOutputStream) {
        this.socket = socket;
        this.dataInputStream = dataInputStream;
        this.dataOutputStream = dataOutputStream;
    }

    public static String getFiles() {
        File directory = new File(".");
        File[] filesList = directory.listFiles();
        String directoryString = "";
        for(File f: filesList) {
            if(f.getName().contains("part")) {
                directoryString += f.getName() + "\n";
            }
        }
        return(directoryString.substring(0, directoryString.length() - 1));
    }

    public void run() {
        String recieved, toReturn = "BOGUS", fileContent = "", filename;
        while(true) {
            try {
                String peerName = (String)peer.peersPeer5.get(socket.getPort());
                String serverFiles = getFiles();
                dataOutputStream.writeUTF(serverFiles);
                recieved = dataInputStream.readUTF();
                if (recieved.equals("Exit")) {
                    System.out.println((String)peer.peersPeer5.get(socket.getPort()) + " is exiting.");
                    this.socket.close();
                    socket.close();
                    System.out.println("Connection Closed.");
                    break;
                }
                else {
                    dataOutputStream.writeUTF("-------------------------------");
                    System.out.println(recieved);
                }
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
        try{
            this.dataInputStream.close();
            this.dataOutputStream.close();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

}
