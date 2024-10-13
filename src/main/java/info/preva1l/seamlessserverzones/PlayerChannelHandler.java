package info.preva1l.seamlessserverzones;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.proxy.connection.client.ConnectedPlayer;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import com.velocitypowered.proxy.protocol.packet.JoinGamePacket;
import com.velocitypowered.proxy.protocol.packet.RespawnPacket;
import com.velocitypowered.proxy.protocol.packet.config.FinishedUpdatePacket;
import com.velocitypowered.proxy.protocol.packet.config.StartUpdatePacket;
import info.preva1l.seamlessserverzones.packet.*;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;

import java.util.HashSet;
import java.util.Set;

public final class PlayerChannelHandler extends ChannelOutboundHandlerAdapter {
    private final Player player;
    private boolean reconnecting = false;
    private boolean reconnectingConfiguration = false;
    private boolean reconnectingUpdateTags = false;
    private boolean reconnectingUpdateRecipes = false;
    private Set<ChunkPos> visibleChunks = new HashSet<>();
    private Set<ChunkPos> reconnectingChunks = new HashSet<>();
    private boolean configurationState = false;

    private boolean needsFlushing = false;

    public PlayerChannelHandler(Player player) {
        this.player = player;
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object packet, ChannelPromise promise) throws Exception {
        if (!needsFlushing) {
            super.write(ctx, packet, promise);
            return;
        }
        if (packet instanceof ByteBuf byteBuf && !configurationState) {
            try {
                tryDecode(ctx, byteBuf, promise);
                return;
            } catch (Exception e) {
                e.printStackTrace();
                throw e;
            }
        }

        if (packet instanceof FinishedUpdatePacket) {
            configurationState = false;

            if (reconnectingConfiguration) {
                ((ConnectedPlayer) player).getConnection().getChannel().pipeline().fireChannelRead(FinishedUpdatePacket.INSTANCE);
                reconnectingConfiguration = false;

                return;
            }
        }

        if (packet instanceof StartUpdatePacket) {
            configurationState = true;
            if (reconnectingConfiguration) {
                ((ConnectedPlayer) player).getConnection().getChannel().pipeline().fireChannelRead(FinishedUpdatePacket.INSTANCE);
                return;
            }
        }

        if (configurationState && reconnectingConfiguration) {
            return; // Don't need to send configuration packets during a seamless reconnect
        }

        if (reconnecting) {
            if (packet instanceof JoinGamePacket) {
                // First velocity sends a JoinGame packet
                return;
            }
            if (packet instanceof RespawnPacket) {
                // Then velocity sends a Respawn packet
                reconnecting = false;
                return;
            }

            //reconnecting = false;
        }

        // send it if we need it
        super.write(ctx, packet, promise);
    }

    private void tryDecode(ChannelHandlerContext ctx, ByteBuf byteBuf, ChannelPromise promise) throws Exception {
        int originalReaderIndex = byteBuf.readerIndex();
        int packetId = ProtocolUtils.readVarInt(byteBuf);

        Object packet = PacketRegistry.readPacket(this.player.getProtocolVersion(), packetId, byteBuf);
        boolean shouldWrite = true;

        if (packet instanceof UpdateTags && reconnectingUpdateTags) {
            // This packet causes a lag spike when reconnecting between servers
            shouldWrite = false;
            reconnectingUpdateTags = false;
        }

        if (packet instanceof UpdateRecipes && reconnectingUpdateRecipes) {
            // This packet causes a lag spike when reconnecting between servers
            shouldWrite = false;
            reconnectingUpdateRecipes = false;
        }

        if (packet instanceof UnloadChunk unloadChunk) {
            // Keep track of loaded chunks
            visibleChunks.remove(unloadChunk.pos());
        }

        if (packet instanceof ChunkData chunkData) {
            // Keep track of loaded chunks
            promise = promise.unvoid();
            promise.addListener(future -> {
                if (future.isSuccess()) {
                    // Only mark the chunk as visible if it actually gets sent
                    visibleChunks.add(chunkData.pos());
                }
            });

            if (reconnectingChunks.remove(chunkData.pos())) {
                // Let's not send the player chunks that they already have loaded during a reconnect as this uses a lot of bandwidth and causes a brief period of lag
                shouldWrite = false;
            }
        }

        if (shouldWrite) {
            byteBuf.readerIndex(originalReaderIndex);
            super.write(ctx, byteBuf, promise);
        } else {
            byteBuf.release();
            promise.setSuccess();
        }
    }

    public void startSeamlessReconnect() {
        reconnecting = true;
        reconnectingConfiguration = true;
        reconnectingUpdateTags = true;
        reconnectingUpdateRecipes = true;
        reconnectingChunks = visibleChunks;
        visibleChunks = new HashSet<>();
        needsFlushing = true;
    }

    public void flush() {
        if (!needsFlushing) return;

        reconnecting = false;
        reconnectingConfiguration = false;
        reconnectingUpdateTags = false;
        reconnectingUpdateRecipes = false;
        reconnectingChunks = new HashSet<>();
        visibleChunks = new HashSet<>();
        needsFlushing = false;
    }
}