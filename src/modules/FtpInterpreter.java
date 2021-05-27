package modules;

import java.io.*;
import java.net.Socket;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public class FtpInterpreter {

    private UserManager userManager = new UserManager();
    private String rootDir = "\\server";
    private String workingDirectory;
    private String someFileName = null;
    private SocketManager socketManager;
    private int dataPort;
    private Socket ctrlSocket;


    public FtpInterpreter(SocketManager socketManager) {
        this.socketManager = socketManager;
        workingDirectory = rootDir;
    }

    /*Parameter @comm_beg(String) is the begining of a command sent by the client
     * eg. USER <SP> <username> <CRLF>. Here, comm_beg will be USER.
     * Because some FTP commands have parameters and others not, you have to check for a
     * parameter if a command is a parameter type command like USER.*/
    public boolean interpret(String command, Socket ctrlSocket) throws IOException {
        boolean returnVal = false;
        this.ctrlSocket = ctrlSocket;

        int spPos = 0; //position of space
        String comBeg;

        if(command.indexOf(' ') != -1){
            spPos = command.indexOf(' ');
            comBeg = command.substring(0, spPos);
        } else
            comBeg = command;

        /*Access control commands*/
        /*Transfer parameter commands*/
        //case "PORT":;
        /*Service commands*/
        switch (comBeg) {
            case "USER" -> {
                System.out.println("User authentification");
                userManager.passwIndex = userManager.userCheck(command.substring(spPos + 1));
                if (userManager.passwIndex == 0)
                    respond("530", null);
                else
                    respond("331", null);

            }
            case "PASS" -> {
                System.out.println("Password checking");
                if (userManager.passCheck(command.substring(spPos + 1), userManager.passwIndex) == 0)
                    respond("530", null);
                else
                    respond("230", null);

            }
            case "CWD" -> {
                String browseTo = command.substring(spPos + 1);

                if (browseTo.equals("/"))
                    workingDirectory = rootDir;
                else {
                    if (browseTo.indexOf('/') != -1) //CWD /dir1/dir2/...
                        workingDirectory = rootDir + '\\' + browseTo.replace('/', '\\'); //client send path like "/directory/file"
                    else {
                        //CWD dir. Here you have to check if the dir the use want to browse is in the current working directory
                        File file = new File(workingDirectory);
                        for (String fileName : file.list()) {
                            if (fileName.equals(browseTo)) {
                                workingDirectory = workingDirectory + '\\' + browseTo; //File path = /.../workingDir/newDir
                                break;
                            }
                        }
                    }
                }

                respond("250", null);
            }
            case "CDUP" -> { // /dir/dir/dir
                if (!workingDirectory.equals(rootDir)) {
                    int bSlashPos = workingDirectory.indexOf('\\');
                    while (workingDirectory.indexOf('\\', bSlashPos + 1) != -1)
                        bSlashPos = workingDirectory.indexOf('\\', bSlashPos + 1);
                    workingDirectory = workingDirectory.substring(0, bSlashPos);
                    respond("200", null);
                } else
                    respond("550", null);
            }
            case "QUIT" -> {
                respond("221", null);
                returnVal = true;

            }
            case "PASV" -> {
                dataPort = socketManager.createDataSocket().getLocalPort();

                String hostAndPort = socketManager.getServerAddress().toString().substring(socketManager.getServerAddress().toString().indexOf('/') + 1);
                //"InetAddress = [hostname/host address]"

                hostAndPort = '(' + hostAndPort.replace('.', ',') + ',' + dataPort / 256 + ',' + dataPort % 256 + ')';

                respond("227", hostAndPort);
            }
            case "TYPE" -> respond("200", null);
            case "RETR" -> {
                respond("150", null);
                File newFile = new File(workingDirectory + '\\' + command.substring(spPos + 1));
                FileInputStream input = new FileInputStream(newFile);
                Socket dataSocket = socketManager.getServerDataSocket().accept();
                OutputStream output = dataSocket.getOutputStream();
                output.write(input.readAllBytes());

                respond("350", null);

                input.close();
                output.close();
                dataSocket.close();
                socketManager.closeDataSocket();
            }
            case "STOR" -> {
                respond("150", null);
                File newFile = new File(workingDirectory + '\\' + command.substring(spPos + 1));
                newFile.createNewFile();
                Socket dataSocket = socketManager.getServerDataSocket().accept();
                InputStream input = dataSocket.getInputStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(input));
                FileOutputStream writer = new FileOutputStream(newFile);

                byte[] inputBuff = input.readAllBytes();

                writer.write(inputBuff);

                if (newFile.exists())
                    respond("250", null);
                else
                    respond("450", null);

                reader.close();
                writer.close();
                dataSocket.close();
                socketManager.closeDataSocket();

            }
            case "RNFR" -> {
                someFileName = command.substring(spPos + 1);
                respond("350", null);
            }
            case "RNTO" -> {
                String newName = command.substring(spPos + 1);
                File oldFile = new File(workingDirectory + '\\' + someFileName);
                File newFile = new File(workingDirectory + '\\' + newName);
                oldFile.renameTo(newFile);
                respond("250", null);
            }
            case "DELE" -> {
                String fileName = command.substring(spPos + 1);
                File file = new File(workingDirectory + '\\' + fileName);
                if (file.delete())
                    respond("250", null);
                else
                    respond("550", null);
            }
            case "MKD" -> {
                String dirName = command.substring(spPos + 1);
                File directory = new File(workingDirectory + '\\' + dirName);
                if (directory.mkdir())
                    respond("200", null);
                else
                    respond("550", null);
            }
            case "PWD" -> {
                if (workingDirectory.equals(rootDir))
                    respond("257", "/");
                else {
                    respond("257", workingDirectory.substring(7).replace('\\', '/'));
                    //for client, root directory is '/' so we have to exclude the name of the real work directory
                }
            }
            case "LIST" -> {
                String dirPath = workingDirectory;

                if (spPos != 0) {
                    dirPath = command.substring(spPos + 1);
                }

                File directory = new File(dirPath);
                String[] fileNames = directory.list();

                DateTimeFormatter dateTimeFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");


                for (int i = 0; i < fileNames.length; i++) {
                    String filePath = workingDirectory + '\\' + fileNames[i];

                    File file = new File(filePath);
                    if (file.isDirectory())
                        fileNames[i] = ZonedDateTime.ofInstant(Instant.ofEpochMilli(file.lastModified()), ZoneId.systemDefault()).format(dateTimeFormat) + " <DIR> " + fileNames[i];
                    else
                        fileNames[i] = ZonedDateTime.ofInstant(Instant.ofEpochMilli(file.lastModified()), ZoneId.systemDefault()).format(dateTimeFormat) + ' ' + file.length() / 8 + ' ' + fileNames[i];
                }

                Socket dataSocket = socketManager.getServerDataSocket().accept();
                respond("150", null);
                PrintWriter writer = new PrintWriter(dataSocket.getOutputStream(), true);

                for (String fileName : fileNames) {
                    writer.println(fileName);
                }

                writer.close();
                dataSocket.close();
                socketManager.closeDataSocket();

                respond("226", null);

            }
            case "SYST" -> respond("215", null);
            default -> respond("504", null);
        }
        return returnVal;
    }

    private void respond(String response, String parameter) throws IOException {
        PrintWriter writer = new PrintWriter(ctrlSocket.getOutputStream(), true);

        //System.out.println(parameter);
        switch (response) {
            case "500" -> writer.println("500 Syntax error, command unrecognized");
            case "501" -> writer.println("501 Syntax error in parameters or arguments");
            case "502" -> writer.println("502 Command not implemented");
            case "503" -> writer.println("503 Bad sequence of commands");
            case "504" -> writer.println("504 Command not implemented for that parameter");
            case "530" -> writer.println("530 Not logged in");
            case "550" -> writer.println("550 Requested action not taken");
            case "331" -> writer.println("331 User name okay, need password");
            case "200" -> writer.println("200 Command okay");
            case "215" -> writer.println("215 " + System.getProperty("os.name"));
            case "227" -> writer.println("227 Entering Passive Mode " + parameter);
            case "230" -> writer.println("230 User logged in, proceed");
            case "257" -> writer.println("257 " + '\"' + parameter + '\"' + " is current directory");
            case "150" -> writer.println("150 File status okay; about to open data connection");
            case "226" -> writer.println("226 Closing data connection");
            case "220" -> writer.println("220 Service ready for new user");
            case "250" -> writer.println("250 Requested file action okay, completed");
            case "350" -> writer.println("350 Requested file action pending further information");
            case "421" -> writer.println("421 Service not available, closing control connection");
            case "450" -> writer.println("450 Requested file action not taken");
            case "221" -> writer.println("221 Closing control connection");
        }
    }
}
