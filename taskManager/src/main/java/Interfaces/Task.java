package Interfaces;

import java.util.ArrayList;

public interface Task {

    boolean addTask(String uri);
    void setUncompleted(String uri);
    boolean completeTask(String uri, ArrayList<String> tasks);
    String getTask();

}
