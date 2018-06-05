package Network;

import io.atomix.catalyst.buffer.BufferInput;
import io.atomix.catalyst.buffer.BufferOutput;
import io.atomix.catalyst.serializer.CatalystSerializable;
import io.atomix.catalyst.serializer.Serializer;

public class GetTaskRep implements CatalystSerializable{

    public String uri;

    public GetTaskRep(){}

    public GetTaskRep(String uriParam){
        uri = uriParam;
    }

    @Override
    public void writeObject(BufferOutput<?> bufferOutput, Serializer serializer) {
        bufferOutput.writeString(uri);
    }

    @Override
    public void readObject(BufferInput<?> bufferInput, Serializer serializer) {
        uri = bufferInput.readString();
    }
}
