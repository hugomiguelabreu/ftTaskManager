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
        if(!tasks.contains(uri))
            return tasks.add(uri);
        else
            return false;
    }

    public boolean tasksContains(String uri){
        return tasks.contains(uri);
    }

    public boolean addTaskIndex(String uri, int index) {
        if (!tasks.contains(uri)){
            if (index == tasks.size()) {
                tasks.add(index, uri);
            } else {
                tasks.set(index, uri);
            }
            return true;
        }

        return false;
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
                if(!tasks.contains(newURI))
                    tasks.add(newURI);
        return onGoing.remove(uri);
    }

    @Override
    public String getTask(){
        String task = null;
        if(tasks.size() > 0) {
            task = tasks.get(0);
            onGoing.add(task);
            tasks.remove(0);
        }
        return task;
    }

    public void moveTaskToOngoing(String uri){
        tasks.remove(uri);
        onGoing.add(uri);
    }

    public void moveOngoingToQueue(String uri){
        onGoing.remove(uri);
        tasks.add(0, uri);
    }

    public void removeOngoing(String uri){
        onGoing.remove(uri);
    }

    public void print() {
        System.out.print("[");
        for (String s : tasks)
            System.out.print(s + ", ");
        System.out.print("]\n[");
        for (String s : onGoing)
            System.out.print(s + ", ");
        System.out.print("]\n");
    }

    public ArrayList<String> getTasks() {
        return tasks;
    }

    public ArrayList<String> getOnGoing() {
        return onGoing;
    }

    public void setTasks(ArrayList<String> tasks) {
        this.tasks = tasks;
    }

    public void setOnGoing(ArrayList<String> onGoing) {
        this.onGoing = onGoing;

    }
}
