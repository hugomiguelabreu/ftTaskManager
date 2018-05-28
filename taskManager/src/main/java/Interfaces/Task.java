package Interfaces;

public interface Task {

    void addTask(String uri);
    void setUncompleted(String uri);
    void completeTask(String uri);
    String getTask();

}
