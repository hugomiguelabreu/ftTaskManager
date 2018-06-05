package Network;

import io.atomix.catalyst.buffer.BufferInput;
import io.atomix.catalyst.buffer.BufferOutput;
import io.atomix.catalyst.serializer.CatalystSerializable;
import io.atomix.catalyst.serializer.Serializer;

public class AddTasksRep implements CatalystSerializable{

    public boolean result;

    public AddTasksRep(){}

    public AddTasksRep(boolean resultParam){
        result = resultParam;
    }

    @Override
    public void writeObject(BufferOutput<?> bufferOutput, Serializer serializer) {
        bufferOutput.writeBoolean(result);
    }

    @Override
    public void readObject(BufferInput<?> bufferInput, Serializer serializer) {
        result = bufferInput.readBoolean();
    }
}
