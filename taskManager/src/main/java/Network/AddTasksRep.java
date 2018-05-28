package Network;

import io.atomix.catalyst.buffer.BufferInput;
import io.atomix.catalyst.buffer.BufferOutput;
import io.atomix.catalyst.serializer.CatalystSerializable;
import io.atomix.catalyst.serializer.Serializer;

public class AddTasksRep implements CatalystSerializable{

    public int id;
    public boolean result;

    public AddTasksRep(){}

    public AddTasksRep(int idParam, boolean resultParam){
        id = idParam;
        result = resultParam;
    }

    @Override
    public void writeObject(BufferOutput<?> bufferOutput, Serializer serializer) {
        bufferOutput.writeInt(id);
        bufferOutput.writeBoolean(result);
    }

    @Override
    public void readObject(BufferInput<?> bufferInput, Serializer serializer) {
        id = bufferInput.readInt();
        result = bufferInput.readBoolean();
    }
}
