import java.util.ArrayList;

public class Data {

    private ArrayList<String> tasks;
    private ArrayList<String> onGoing;

    public Data(){
        tasks = new ArrayList<>();
        onGoing = new ArrayList<>();
    }

    public void addTask(String uri){
        tasks.add(uri);
    }

    public void setUncompleted(String uri){
        onGoing.remove(uri);
        tasks.add(0, uri);
    }

    public void completeTask(String uri){
        onGoing.remove(uri);
    }

    public String getTask(){
        onGoing.add(tasks.get(0));
        return tasks.remove(0);
    }
}
