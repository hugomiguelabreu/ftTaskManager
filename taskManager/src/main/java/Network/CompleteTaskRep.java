package Network;

import io.atomix.catalyst.buffer.BufferInput;
import io.atomix.catalyst.buffer.BufferOutput;
import io.atomix.catalyst.serializer.CatalystSerializable;
import io.atomix.catalyst.serializer.Serializer;

public class CompleteTaskRep implements CatalystSerializable {

    public int id;
    public boolean response;

    public CompleteTaskRep(){}

    public CompleteTaskRep(int idParam, boolean responseParam){
        id = idParam;
        response = responseParam;
    }

    @Override
    public void writeObject(BufferOutput<?> bufferOutput, Serializer serializer) {
        bufferOutput.writeInt(id);
        bufferOutput.writeBoolean(response);
    }

    @Override
    public void readObject(BufferInput<?> bufferInput, Serializer serializer) {
        id = bufferInput.readInt();
        response = bufferInput.readBoolean();
    }
}
