package Network;

import io.atomix.catalyst.buffer.BufferInput;
import io.atomix.catalyst.buffer.BufferOutput;
import io.atomix.catalyst.serializer.CatalystSerializable;
import io.atomix.catalyst.serializer.Serializer;

public class CompleteTaskRep implements CatalystSerializable {

    public boolean response;
    public String id;

    public CompleteTaskRep(){}

    public CompleteTaskRep(String idParam, boolean responseParam){
        response = responseParam;
        id = idParam;
    }

    @Override
    public void writeObject(BufferOutput<?> bufferOutput, Serializer serializer) {
        bufferOutput.writeBoolean(response);
        bufferOutput.writeString(id);
    }

    @Override
    public void readObject(BufferInput<?> bufferInput, Serializer serializer) {
        response = bufferInput.readBoolean();
        id = bufferInput.readString();
    }
}
