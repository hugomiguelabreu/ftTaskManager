package Network;

import io.atomix.catalyst.buffer.BufferInput;
import io.atomix.catalyst.buffer.BufferOutput;
import io.atomix.catalyst.serializer.CatalystSerializable;
import io.atomix.catalyst.serializer.Serializer;

import java.util.ArrayList;

public class CompleteTaskReq implements CatalystSerializable{

    public String uri;
    public int size;
    public ArrayList<String> tasks;

    public CompleteTaskReq(){}

    public CompleteTaskReq(String uriParam, ArrayList<String> tasksParam){
        uri = uriParam;
        tasks = tasksParam;
        size = tasks.size();
    }

    @Override
    public void writeObject(BufferOutput<?> bufferOutput, Serializer serializer) {
        bufferOutput.writeString(uri);
        bufferOutput.writeInt(size);
        for (String task: tasks)
            bufferOutput.writeString(task);
    }

    @Override
    public void readObject(BufferInput<?> bufferInput, Serializer serializer) {
        uri = bufferInput.readString();
        size = bufferInput.readInt();
        for (int i=0; i<size; i++)
            tasks.add(bufferInput.readString());
    }
}
