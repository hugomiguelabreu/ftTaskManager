package Network;

import io.atomix.catalyst.buffer.BufferInput;
import io.atomix.catalyst.buffer.BufferOutput;
import io.atomix.catalyst.serializer.CatalystSerializable;
import io.atomix.catalyst.serializer.Serializer;

public class AddTasksRep implements CatalystSerializable{

    public String id;
    public boolean result;

    public AddTasksRep(){}

    public AddTasksRep(String idParam, boolean resultParam){
        result = resultParam;
        id = idParam;
    }

    @Override
    public void writeObject(BufferOutput<?> bufferOutput, Serializer serializer) {
        bufferOutput.writeBoolean(result);
        bufferOutput.writeString(id);
    }

    @Override
    public void readObject(BufferInput<?> bufferInput, Serializer serializer) {
        result = bufferInput.readBoolean();
        id = bufferInput.readString();
    }
}
