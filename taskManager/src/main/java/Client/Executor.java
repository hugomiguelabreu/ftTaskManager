package Client;

import Interfaces.Task;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.ArrayList;

public class Executor extends Thread {

    private boolean stop;
    private Task tc;


    public Executor(Task t){
        stop = false;
        tc = t;
    }

    @Override
    public void run() {
        //Enquanto não houver sinal para parar
        //de tratar pedidos vamos continuar;
        while (!stop){
            String newUrl = tc.getTask();
            ArrayList<String> newTasks = scrappe(newUrl);
            for (String k : newTasks)
                System.out.println(k);
            tc.completeTask(newUrl, newTasks);
        }
    }

    public void finish(){
        stop = true;
    }

    private ArrayList<String> scrappe(String url){
        ArrayList<String> result = new ArrayList<>();
        Document doc = null;

        try {
            doc = Jsoup.connect(url).get();
            System.out.println(doc.title());

            Elements links = doc.select("a");
            for (Element link : links) {
                String absHref = link.attr("abs:href"); // "http://jsoup.org/"
                if(!result.contains(absHref) && !absHref.equals(""))
                    result.add(absHref);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        return result;
    }
}
