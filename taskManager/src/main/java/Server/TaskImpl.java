package Server;

import Interfaces.Task;

import java.util.ArrayList;

public class TaskImpl implements Task{
    private ArrayList<String> tasks;
    private ArrayList<String> onGoing;

    public TaskImpl(){
        tasks = new ArrayList<>();
        onGoing = new ArrayList<>();
    }

    @Override
    public boolean addTask(String uri){
        tasks.add(uri);
        return false;
    }

    @Override
    public void setUncompleted(String uri){
        onGoing.remove(uri);
        tasks.add(0, uri);
    }

    @Override
    public void completeTask(String uri){
        onGoing.remove(uri);
    }

    @Override
    public String getTask(){
        onGoing.add(tasks.get(0));
        return tasks.remove(0);
    }
}
