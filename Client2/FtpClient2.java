import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ConnectException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;

public class FtpClient2 {

public static void main(String[] args)  {
    FtpClient2 client2=new FtpClient2();
    client2.run();
}

void run(){
    String serverHost = null;
    String serverPortNumber = null;
    ObjectOutputStream clientSocketPrintWriter = null;
    ObjectInputStream clientSocketReader = null;
    BufferedReader inputReader = new BufferedReader(new InputStreamReader(System.in));;
    Socket socket = null;
    try {
        while(true){
            System.out.println("Please enter ftp command:");
            String command=inputReader.readLine();
            String[] ftpConnectCommand=command.split(" ");
            if(ftpConnectCommand.length==3 && ftpConnectCommand[0].equals("ftpclient")){
                if(ftpConnectCommand[1].equals("localhost")){
                    serverHost=ftpConnectCommand[1];
                    serverPortNumber=ftpConnectCommand[2];
                    break;
                }else{
                    System.out.println("Invalid Host! Please enter correct host");
                }
            }else{
                System.out.println("Invalid Command! It needs 3 inputs in format: ftpclient <serverHost> <portNumber> ");
            }
        }
        socket = new Socket(serverHost, Integer.parseInt(serverPortNumber));
        if (socket != null) {
            System.out.println("Connected to " + serverHost + " on port Number " + serverPortNumber);
        }
        clientSocketPrintWriter = new ObjectOutputStream(socket.getOutputStream());
        clientSocketReader = new ObjectInputStream(socket.getInputStream());
        authenticateClient(clientSocketPrintWriter, clientSocketReader, inputReader);
        handleClientInputs(clientSocketPrintWriter, clientSocketReader, inputReader);
    } catch (ConnectException e) {
        System.err.println("Connection refused. You need to initiate a server first.");
    }
    catch ( ClassNotFoundException e ) {
        System.err.println("Class not found");
    }
    catch(UnknownHostException unknownHost){
        System.err.println("You are trying to connect to an unknown host!");
    }
    catch(IOException ioException){
        ioException.printStackTrace();
    }
    finally {
        try {
            if(clientSocketPrintWriter!=null && clientSocketReader!=null && inputReader!=null && socket!=null){
                clientSocketPrintWriter.close();
                clientSocketReader.close();
                inputReader.close();
                socket.close();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}

    public static void authenticateClient(ObjectOutputStream clientSocketPrintWriter, ObjectInputStream clientSocketReader,
                                          BufferedReader inputReader) throws IOException, ClassNotFoundException {
        while (true) {
            System.out.println("Please enter userName and password separated by space");
            String usrNamePassword = inputReader.readLine();
            if (usrNamePassword.contains(" ")) {
                String[] userNamePassword = usrNamePassword.split(" ");
                clientSocketPrintWriter.writeObject(userNamePassword[0]);
                clientSocketPrintWriter.writeObject(userNamePassword[1]);
                clientSocketPrintWriter.flush();
            } else {
                System.out.println("Invalid Input");
                continue;
            }
            String line = (String) clientSocketReader.readObject();
            if (line.equals("Authentication Successful")) {
                System.out.println(line);
                break;
            } else {
                System.out.println(line);
            }
        }
    }

    public static void handleClientInputs(ObjectOutputStream clientSocketPrintWriter, ObjectInputStream clientSocketReader,
                                          BufferedReader inputReader) throws IOException, ClassNotFoundException {
        while (true) {
            System.out.println("Please input Ftp Commands:");
            String ftpCommand = inputReader.readLine();
            clientSocketPrintWriter.writeObject(ftpCommand);
            clientSocketPrintWriter.flush();
            if (validateCommand(clientSocketReader)) {
                if (ftpCommand.startsWith("dir")) {
                    String ouput = (String) clientSocketReader.readObject();
                    System.out.println(ouput);
                } else if (ftpCommand.startsWith("get")) {
                    String isFileAvailable = (String) clientSocketReader.readObject();
                    if (isFileAvailable.equals("true")) {
                        System.out.println("Downloading....");
                        File file = new File(ftpCommand.split(" ")[1]);
                        FileOutputStream out = new FileOutputStream(file);
                        String fileChar = "";
                        while (!(fileChar = (String) clientSocketReader.readObject()).equals(String.valueOf(-1))) {
                            out.write(Integer.parseInt(fileChar));
                        }
                        System.out.println("File written sucessfully");
                    } else {
                        System.out.println("File is not present on Server");
                    }
                } else if (ftpCommand.startsWith("upload")) {
                    if (isFileAvailableOnMachine(ftpCommand, clientSocketPrintWriter)) {
                        System.out.println("Uploading....");
                        FileInputStream in = new FileInputStream(new File(ftpCommand.split(" ")[1]));
                        int x = 0;
                        while ((x = in.read()) != -1) {
                            clientSocketPrintWriter.writeObject(String.valueOf(x));
                            clientSocketPrintWriter.flush();
                        }
                        clientSocketPrintWriter.writeObject(String.valueOf(-1));
                        clientSocketPrintWriter.flush();
                        System.out.println((String) clientSocketReader.readObject());
                    } else {
                        System.out.println("file is not present on the machine");
                    }
                }
            } else {
                System.out.println("Invalid Command,Please enter valid command");
            }

        }
    }

    public static boolean validateCommand(ObjectInputStream clientSocketReader) throws IOException, ClassNotFoundException {
        String isCommandValid = (String) clientSocketReader.readObject();
        if (isCommandValid.equals("true")) {
            return true;
        } else {
            return false;
        }
    }

    public static boolean isFileAvailableOnMachine(String command, ObjectOutputStream clientSocketPrintWriter) throws IOException {
        String fileName = command.split(" ")[1];
        File file = new File(".");
        File[] files = file.listFiles();
        Map<String, File> fileNamesMap = new HashMap<>();
        for (File dirfile : files) {
            fileNamesMap.put(dirfile.getName(), dirfile);
        }
        if (fileNamesMap.keySet().contains(fileName)) {
            clientSocketPrintWriter.writeObject("true");
            clientSocketPrintWriter.flush();
            return true;
        } else {
            clientSocketPrintWriter.writeObject("false");
            clientSocketPrintWriter.flush();
            return false;
        }
    }
}