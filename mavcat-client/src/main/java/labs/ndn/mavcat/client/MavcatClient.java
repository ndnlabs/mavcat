package labs.ndn.mavcat.client;

import labs.ndn.mavcat.client_sdk.download.DownloadTaskManager;

import java.util.Scanner;

public class MavcatClient {



    public static void main(String args[]) {
        // Initial task
        DownloadTaskManager.scheduleWriteDownloadInfo();

        Scanner reader = new Scanner(System.in);

        while (reader.hasNextLine()) {
            String line = reader.nextLine();

            System.out.println(CommandParser.parseCommand(line));
        }

    }

    public static void parseCommand(String line) {

    }

}
