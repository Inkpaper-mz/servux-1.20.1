package fi.dy.masa.servux.network.packet;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

import fi.dy.masa.servux.dataproviders.LitematicsDataProvider;
import fi.dy.masa.servux.network.IPluginChannelHandler;
import fi.dy.masa.servux.network.PacketSplitter;

/**
 * Handles the {@code servux:litematics} channel on the server side (1.20.1 networking API).
 */
public class ServuxLitematicaHandler implements IPluginChannelHandler
{
    public static final ServuxLitematicaHandler INSTANCE = new ServuxLitematicaHandler();

    private ServuxLitematicaHandler()
    {
    }

    public static ServuxLitematicaHandler getInstance()
    {
        return INSTANCE;
    }

    @Override
    public Identifier getChannel()
    {
        return ServuxLitematicaPacket.CHANNEL;
    }

    @Override
    public boolean usePacketSplitter()
    {
        return true;
    }

    @Override
    public boolean isSubscribable()
    {
        return true;
    }

    @Override
    public boolean subscribe(ServerPlayNetworkHandler netHandler)
    {
        return true;
    }

    @Override
    public boolean unsubscribe(ServerPlayNetworkHandler netHandler)
    {
        return true;
    }

    @Override
    public void onPacketReceived(PacketByteBuf buf, ServerPlayNetworkHandler netHandler)
    {
        ServerPlayerEntity player = netHandler.player;
        ServuxLitematicaPacket packet = ServuxLitematicaPacket.fromPacket(buf);

        if (packet == null || player == null)
        {
            return;
        }

        switch (packet.getType())
        {
            case PACKET_C2S_METADATA_REQUEST:
                LitematicsDataProvider.INSTANCE.sendMetadata(player);
                break;
            case PACKET_C2S_PASTE_REQUEST:
                LitematicsDataProvider.INSTANCE.handlePasteRequest(player, packet.getCompound());
                break;
            default:
                break;
        }
    }

    public void sendMetadata(ServerPlayerEntity player)
    {
        this.send(player, ServuxLitematicaPacket.MetadataResponse(LitematicsDataProvider.INSTANCE.metadata));
    }

    public void send(ServerPlayerEntity player, ServuxLitematicaPacket packet)
    {
        PacketSplitter.send(ServuxLitematicaPacket.CHANNEL, ServuxLitematicaPacket.toBuffer(packet), player);
    }

    /**
     * Compatibility method used by the ported schematic transmit code.
     */
    public void encodeServerData(ServerPlayerEntity player, ServuxLitematicaPacket packet)
    {
        this.send(player, packet);
    }
}
