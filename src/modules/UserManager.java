package modules;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Scanner;

public class UserManager {
    private String pathToCount = "\\userCount.txt"; //number of users registered
    private String pathToCred = "\\credentials.txt"; //Path to the file where the credentials are stored
    private int userCount = 0;
    public int passwIndex = 0;

    /*Parameter @return(int): 0 - if user was not match
     another int - corresponding to the password index in the file, if user was not match*/
    public int userCheck(String username) throws IOException {
        File credentials = new File(pathToCred);
        Scanner read = new Scanner(credentials);

        while(read.hasNext()){
            String readed = read.nextLine();
            if(readed.contains(username)){
                int sp_pos = readed.indexOf(32);
                return Integer.parseInt(readed.substring(0, sp_pos));
            }
        }

        read.close();
        return 0;
    }

    /*Parameter @return(int): 0 - if pass was not match. 1 - if user was match*/
    public int passCheck(String password, int passIndex) throws IOException{
        File credentials = new File(pathToCred);
        Scanner read = new Scanner(credentials);

        while(read.hasNext()){
            String readed = read.nextLine();
            if(readed.contains(password)){
                int sp_pos = readed.indexOf(32);
                if(Integer.parseInt(readed.substring(0, sp_pos)) == passIndex)
                    return 1;
            }
        }

        read.close();
        return 0;
    }

    public void addUser() throws IOException{
        System.out.println("Please enter the username and the password separated by space:");
        Scanner inputScan = new Scanner(System.in);
        String credentials = inputScan.nextLine();
        inputScan.close();

        File credentials_db = new File(pathToCred);
        File userCount_db = new File(pathToCount);
        if (credentials_db.createNewFile() && userCount_db.createNewFile()){
            System.out.println("File containing credentials was created");
            FileWriter writer = new FileWriter(pathToCount);
            userCount = 0;
            writer.write(Integer.toString(userCount));
            writer.close();
        } else {
            Scanner scan = new Scanner(userCount_db);
            userCount = scan.nextInt();
            scan.close();
        }

        FileWriter writer = new FileWriter(credentials_db, true);
        writer.write(Integer.toString(++userCount) + ' ' + credentials + "\r\n");
        writer.close();

        //updating number of users
        writer = new FileWriter(userCount_db);
        writer.write(Integer.toString(userCount));
        writer.close();
    }
}
