import java.io.*;
import java.util.*;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Paths;

public class peer {

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
      getNumChunks();
      System.out.println(nChunks);
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
                    Socket socketPeer1Client = new Socket(InetAddress.getByName("localhost"), 7004);
                    DataInputStream dataInputStreamPeer1Client = new DataInputStream(socketPeer1Client.getInputStream());
                    DataOutputStream dataOutputStreamPeer1Client = new DataOutputStream(socketPeer1Client.getOutputStream());
                    while(true) {
                        String clientFiles = getFiles();
                        String serverFiles = dataInputStreamPeer1Client.readUTF();
                        String[] serverFileList = serverFiles.split("\n");
                        if(hasAllFiles()) {
                            dataOutputStreamPeer1Client.writeUTF("Exit");
                            System.out.println("Receieved all parts.\nClosing this connection.");
                            mergeAllParts();
                            socketPeer1Client.close();
                            System.out.println("Connection Closed.");
                            break;
                        }
                        else {
                            for(String fileName: serverFileList) {
                                if(!clientFiles.contains(fileName)) {
                                    System.out.println("Requesting for file " + fileName + " from Peer4");
                                    String source = "../peer4/" + fileName;
                                    File destination = new File(fileName);
                                    Files.copy(Paths.get(source), destination.toPath());
                                    dataOutputStreamPeer1Client.writeUTF("Sending file " + fileName + " to Peer1");
                                }
                            }
                            //dataOutputStreamPeer1Client.writeUTF("OOLALA");
                            String r = dataInputStreamPeer1Client.readUTF();
                            System.out.println(r);
                        }
                    }
                    dataInputStreamPeer1Client.close();
                    dataOutputStreamPeer1Client.close();
                }
                catch(Exception e) {
                    e.printStackTrace();
                }
            }
        };
        /*Thread thread2 = new Thread () {
            public void run () {
                for(int i = 1; i <= 10; i ++) {
                    System.out.println("In thread2: " + i);
                }
            }
        };*/
        thread1.start();
        //thread2.start();
    }

}
