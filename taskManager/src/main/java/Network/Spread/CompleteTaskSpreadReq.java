package Network.Spread;

import io.atomix.catalyst.buffer.BufferInput;
import io.atomix.catalyst.buffer.BufferOutput;
import io.atomix.catalyst.serializer.CatalystSerializable;
import io.atomix.catalyst.serializer.Serializer;

import java.util.ArrayList;

public class CompleteTaskSpreadReq implements CatalystSerializable{

    public String id;
    public String clientuid;
    public String uri;
    public int size;
    public ArrayList<String> tasks;
    public ArrayList<Integer> indexes;

    public CompleteTaskSpreadReq(){}

    public CompleteTaskSpreadReq(String idParam, String clientuidParam, String uriParam, ArrayList<String> tasksParam,
                                    ArrayList<Integer> indexesParam){
        uri = uriParam;
        clientuid = clientuidParam;
        tasks = tasksParam;
        size = tasks.size();
        id = idParam;
        indexes = indexesParam;
    }

    @Override
    public void writeObject(BufferOutput<?> bufferOutput, Serializer serializer) {
        bufferOutput.writeString(id);
        bufferOutput.writeString(clientuid);
        bufferOutput.writeString(uri);
        bufferOutput.writeInt(size);
        for (String task: tasks)
            bufferOutput.writeString(task);
        for (Integer index: indexes)
            bufferOutput.writeInt(index);
    }

    @Override
    public void readObject(BufferInput<?> bufferInput, Serializer serializer) {
        id = bufferInput.readString();
        clientuid = bufferInput.readString();
        tasks = new ArrayList<>();
        indexes = new ArrayList<>();
        uri = bufferInput.readString();
        size = bufferInput.readInt();
        for (int i=0; i<size; i++)
            tasks.add(bufferInput.readString());
        for (int i=0; i<size; i++)
            indexes.add(bufferInput.readInt());
    }
}
