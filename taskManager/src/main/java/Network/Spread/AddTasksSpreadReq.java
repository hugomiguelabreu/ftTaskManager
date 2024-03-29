package Network.Spread;

import io.atomix.catalyst.buffer.BufferInput;
import io.atomix.catalyst.buffer.BufferOutput;
import io.atomix.catalyst.serializer.CatalystSerializable;
import io.atomix.catalyst.serializer.Serializer;

public class AddTasksSpreadReq implements CatalystSerializable{

    public String id;
    public String uri;
    public int index;

    public AddTasksSpreadReq(){}

    public AddTasksSpreadReq(String idParam, String uriParam, int indexParam){
        uri = uriParam;
        id = idParam;
        index = indexParam;
    }

    @Override
    public void writeObject(BufferOutput<?> bufferOutput, Serializer serializer) {
        bufferOutput.writeString(uri);
        bufferOutput.writeString(id);
        bufferOutput.writeInt(index);
    }

    @Override
    public void readObject(BufferInput<?> bufferInput, Serializer serializer) {
        uri = bufferInput.readString();
        id  = bufferInput.readString();
        index = bufferInput.readInt();
    }
}
