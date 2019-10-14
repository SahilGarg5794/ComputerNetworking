import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class FtpServer {

    private static final int portNumber=8081;
    private static final Map<String,String> credentialsMap= Stream.of(
            new AbstractMap.SimpleImmutableEntry<>("username1","password1"),
            new AbstractMap.SimpleImmutableEntry<>("username2","password2"),
            new AbstractMap.SimpleImmutableEntry<>("username3","password3"),
            new AbstractMap.SimpleImmutableEntry<>("username4","password4"),
            new AbstractMap.SimpleImmutableEntry<>("username5","password5")
    ).collect(Collectors.toMap(AbstractMap.SimpleImmutableEntry::getKey, AbstractMap.SimpleImmutableEntry::getValue));

    private static final List<String> listOfCommands=new ArrayList<>(Arrays.asList("get","dir","upload"));

    public void createSocketAndCommunicate(){
        int clientCounter=1;
        ServerSocket  serverSocket=null;
        Socket clientSocket=null;
        try{
            serverSocket=new ServerSocket(portNumber);
            System.out.println("Waiting for connection..");
            while(true){
                clientSocket= serverSocket.accept();
                communicateWithClient(clientSocket,clientCounter++);
            }
        }catch(Exception e){
            e.printStackTrace();
        }finally {
            try {
                serverSocket.close();
                clientSocket.close();
            } catch (IOException e) {
                System.out.println("Client Disconnected!");
            }
        }
    }

    public void communicateWithClient(Socket clientSocket,int clientNumber){

        Thread thread =new Thread(new Runnable() {
            @Override
            public void run() {
                System.out.println("connection received from client "+clientNumber);
                try(ObjectInputStream clientSocketReader=new ObjectInputStream(clientSocket.getInputStream());
                    ObjectOutputStream clientWriter=new ObjectOutputStream(clientSocket.getOutputStream());){
                    validateCredentials(clientSocketReader,clientWriter);
                    getCommandsFromClient(clientSocketReader,clientWriter);
                } catch (IOException e) {
                    System.out.println("Client Disconnected!");
                } catch (ClassNotFoundException e) {
                    System.out.println("Client Disconnected!");
                }
            }
        });
        thread.start();
    }

    public void validateCredentials(ObjectInputStream clientSocketReader,ObjectOutputStream clientWriter) throws IOException, ClassNotFoundException {
        while(true){
            String userName=(String)clientSocketReader.readObject();
            String passWord=(String)clientSocketReader.readObject();
            if(credentialsMap.keySet().contains(userName) && credentialsMap.values().contains(passWord)){
                clientWriter.writeObject("Authentication Successful");
                clientWriter.flush();
                break;
            }else{
                clientWriter.writeObject("Authentication Failed, Please re-enter username and password");
                clientWriter.flush();
            }
        }
    }

    private void getCommandsFromClient(ObjectInputStream clientSocketReader,ObjectOutputStream clientWriter) throws IOException, ClassNotFoundException {
        while (true) {
            String command = (String) clientSocketReader.readObject();
            boolean isCommandValid = validateCommand(command, clientWriter);
            if (isCommandValid) {
                switch (command.split(" ")[0]) {
                    case "dir":
                        String output = " ";
                        File file = new File(".");
                        File[] files = file.listFiles();
                        for (File dirfile : files) {
                            output += dirfile.getName() + "\n";
                        }
                        clientWriter.writeObject(output);
                        clientWriter.flush();
                        break;
                    case "upload":
                        if(((String)clientSocketReader.readObject()).equals("true")){
                            File uploadFile = new File(command.split(" ")[1]);
                            FileOutputStream out = new FileOutputStream(uploadFile);
                            String fileChar = "";
                            while (!(fileChar = (String) clientSocketReader.readObject()).equals(String.valueOf(-1))) {
                                out.write(Integer.parseInt(fileChar));
                            }
                            clientWriter.writeObject("File uploaded Successfully");
                            clientWriter.flush();
                        }
                        break;
                    case "get":
                        handleGetCommand(command, clientWriter);
                        break;
                }
            }
        }
    }


    public static void handleGetCommand(String command,ObjectOutputStream clientWriter)
            throws IOException, ClassNotFoundException{
        boolean isFilePresent=isFilePresentOnServer(command);
        if(isFilePresent) {
            clientWriter.writeObject("true");
            clientWriter.flush();
            FileInputStream in=new FileInputStream(command.split(" ")[1]);
            int x=0;
            while((x=in.read())!=-1){
                clientWriter.writeObject(String.valueOf(x));
                clientWriter.flush();
            }
            clientWriter.writeObject(String.valueOf(-1));
            clientWriter.flush();
        }else {
            clientWriter.writeObject("false");
            clientWriter.flush();
        }
    }

    public static boolean isFilePresentOnServer(String command) {
        String fileName=command.split(" ")[1];
        File file=new File(".");
        File[] files = file.listFiles();
        Map<String,File> fileNamesMap=new HashMap<>();
        for(File dirfile:files){
            fileNamesMap.put(dirfile.getName(),dirfile);
        }
        if(fileNamesMap.keySet().contains(fileName)){
            return true;
        }else {
            return false;
        }
    }


    public static boolean validateCommand(String command,ObjectOutputStream clientWriter) throws IOException{
        String[] commandArray = command.split(" ");
        boolean status=false;
        if(listOfCommands.contains(commandArray[0])){
            if(commandArray[0].equals("dir") && commandArray.length==1){
                clientWriter.writeObject("true");
                clientWriter.flush();
                status=true;
            }else if(commandArray[0].equals("get") && commandArray.length==2){
                clientWriter.writeObject("true");
                clientWriter.flush();
                status=true;
            }else if(commandArray[0].equals("upload") && commandArray.length==2){
                clientWriter.writeObject("true");
                clientWriter.flush();
                status=true;
            }else{
                System.out.println("command is invalid");
                clientWriter.writeObject("false");
                clientWriter.flush();
            }
            return status;
        }else{
            clientWriter.writeObject("false");
            clientWriter.flush();
            return status;
        }
    }
    public static void main(String[] args) {
        FtpServer server=new FtpServer();
        server.createSocketAndCommunicate();
    }

}
