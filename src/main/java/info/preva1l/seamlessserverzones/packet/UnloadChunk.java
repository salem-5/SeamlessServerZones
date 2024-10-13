package info.preva1l.seamlessserverzones.packet;

import com.velocitypowered.api.network.ProtocolVersion;
import info.preva1l.seamlessserverzones.ChunkPos;
import io.netty.buffer.ByteBuf;

public class UnloadChunk {

    private final ChunkPos pos;

    public UnloadChunk(ByteBuf byteBuf, ProtocolVersion protocolVersion) {
        this.pos = new ChunkPos(byteBuf.readInt(), byteBuf.readInt());
    }

    public ChunkPos pos() {
        return pos;
    }
}