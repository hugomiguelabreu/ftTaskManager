package Network;

import io.atomix.catalyst.buffer.BufferInput;
import io.atomix.catalyst.buffer.BufferOutput;
import io.atomix.catalyst.serializer.CatalystSerializable;
import io.atomix.catalyst.serializer.Serializer;

public class GetTaskRep implements CatalystSerializable{

    public String id;
    public String uri;

    public GetTaskRep(){}

    public GetTaskRep(String idParam, String uriParam){
        uri = uriParam;
        id = idParam;
    }

    @Override
    public void writeObject(BufferOutput<?> bufferOutput, Serializer serializer) {
        bufferOutput.writeString(uri);
        bufferOutput.writeString(id);
    }

    @Override
    public void readObject(BufferInput<?> bufferInput, Serializer serializer) {
        uri = bufferInput.readString();
        id = bufferInput.readString();
    }
}
