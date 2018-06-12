package Server;

import Interfaces.Task;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentLinkedDeque;

public class TaskImpl implements Task{
    private ArrayList<String> tasks;
    private ArrayList<String> onGoing;

    public TaskImpl(){
        tasks = new ArrayList<>();
        onGoing = new ArrayList<>();
    }

    @Override
    public boolean addTask(String uri){
        return tasks.add(uri);
    }

    public int taskIndex(String uri){
        return tasks.indexOf(uri);
    }

    @Override
    public void setUncompleted(String uri){
        if(onGoing.remove(uri))
            tasks.add(0, uri);
    }

    @Override
    public boolean completeTask(String uri, ArrayList<String> newTasks){
        //Existem novas tasks associadas a este uri??
        if(newTasks != null)
            for (String newURI: newTasks)
                tasks.add(newURI);

        return onGoing.remove(uri);
    }

    @Override
    public String getTask(){
        String task = tasks.get(0);
        if(task != null) {
            onGoing.add(task);
            tasks.remove(0);
        }
        return task;
    }
}
