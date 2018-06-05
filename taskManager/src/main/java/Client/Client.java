package Client;

import Interfaces.Task;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Scanner;

public class Client {

    public static void main(String[] args) throws IOException, InterruptedException {

        Task ts =  new TaskStub();
        Executor e = new Executor(ts);
        Scanner read = new Scanner(System.in);
        String line;

        System.out.println("[1] New task");
        System.out.println("[2] Execute tasks given by the server");
        System.out.println("[3] Quit normally");
        System.out.println("[4] Quit unexpected");

        while ((line = read.nextLine()) != null){
            int selected = Integer.parseInt(line);
            switch (selected){
                case 1:
                    System.out.print("URL:");
                    ts.addTask(read.nextLine());
                    break;
                case 2:
                    e.start();
                    break;
                case 3:
                    e.finish();
                    e.join();
                    System.exit(0);
                    break;
                case 4:
                    System.exit(1);
                    break;
            }
        }

    }

}
