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
            System.out.println("GET");
            String newUrl = tc.getTask();
            System.out.println("GET DONE");
            if(newUrl != null) {
                ArrayList<String> newTasks = scrappe(newUrl);
                boolean result = tc.completeTask(newUrl, newTasks);
                System.out.println(newUrl + " completed: " + result);
            }else{
                //Espera por novos trabalhos / Novos servidores
                try {
                    System.out.println("NO WORK. WAITING 5 SEC.");
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
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
