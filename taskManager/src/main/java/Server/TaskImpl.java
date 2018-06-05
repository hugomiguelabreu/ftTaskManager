package Server;

import Interfaces.Task;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentLinkedDeque;

public class TaskImpl implements Task{
    private ConcurrentLinkedDeque<String> tasks;
    private ConcurrentLinkedDeque<String> onGoing;

    public TaskImpl(){
        tasks = new ConcurrentLinkedDeque<>();
        onGoing = new ConcurrentLinkedDeque<>();
    }

    @Override
    public boolean addTask(String uri){
        return tasks.add(uri);
    }

    @Override
    public void setUncompleted(String uri){
        if(onGoing.remove(uri))
            tasks.addFirst(uri);
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
        String task = tasks.poll();
        if(task != null)
            onGoing.add(task);
        return task;
    }
}
