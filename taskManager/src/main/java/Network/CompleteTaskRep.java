package Network;

import io.atomix.catalyst.buffer.BufferInput;
import io.atomix.catalyst.buffer.BufferOutput;
import io.atomix.catalyst.serializer.CatalystSerializable;
import io.atomix.catalyst.serializer.Serializer;

public class CompleteTaskRep implements CatalystSerializable {

    public boolean response;

    public CompleteTaskRep(){}

    public CompleteTaskRep(boolean responseParam){
        response = responseParam;
    }

    @Override
    public void writeObject(BufferOutput<?> bufferOutput, Serializer serializer) {
        bufferOutput.writeBoolean(response);
    }

    @Override
    public void readObject(BufferInput<?> bufferInput, Serializer serializer) {
        response = bufferInput.readBoolean();
    }
}
