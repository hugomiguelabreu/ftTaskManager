package Client;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import java.io.IOException;
import java.util.ArrayList;

public class Client {

    public static void main(String[] args) throws IOException {

        ArrayList<String> uniqueLinks = new ArrayList<>();
        Document doc = Jsoup.connect(args[0]).get();
        System.out.println(doc.title());

        Elements links = doc.select("a");
        for (Element link : links) {
            String absHref = link.attr("abs:href"); // "http://jsoup.org/"
            if(!uniqueLinks.contains(absHref) && !absHref.equals(""))
                uniqueLinks.add(absHref);
        }

        for (String k : uniqueLinks)
            System.out.println(k);

    }

}
