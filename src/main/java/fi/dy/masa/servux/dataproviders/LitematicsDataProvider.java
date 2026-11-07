package fi.dy.masa.servux.dataproviders;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.annotation.Nullable;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.BlockMirror;
import net.minecraft.util.BlockRotation;

import fi.dy.masa.servux.Servux;
import fi.dy.masa.servux.network.IPluginChannelHandler;
import fi.dy.masa.servux.network.packet.ServuxLitematicaHandler;
import fi.dy.masa.servux.network.packet.ServuxLitematicaPacket;
import fi.dy.masa.servux.schematic.placement.SchematicPlacement;
import fi.dy.masa.servux.schematic.transmit.SchematicBufferManager;
import fi.dy.masa.servux.util.PasteLayerBehavior;
import fi.dy.masa.servux.util.ReplaceBehavior;

/**
 * Server side data provider for Litematica.
 * <p>
 * Handles the schematic paste requests sent by the Litematica 1.20.1 client port.
 */
public class LitematicsDataProvider extends DataProviderBase
{
    public static final LitematicsDataProvider INSTANCE = new LitematicsDataProvider();

    public final NbtCompound metadata = new NbtCompound();
    public boolean fixRaiLRotations = true;
    public boolean fixStairMirror = true;
    public boolean fixChestMirror = true;

    private final String permNode = "servux.provider.litematic_data";
    private int permissionLevel = 0;
    private int pastePermissionLevel = 0;

    private final SchematicBufferManager bufferManager = new SchematicBufferManager();
    @Nullable private Path transmitDir;

    protected LitematicsDataProvider()
    {
        super("litematic_data",
              ServuxLitematicaPacket.CHANNEL,
              ServuxLitematicaPacket.PROTOCOL_VERSION,
              "Litematica server-side schematic paste support");

        this.enabled = true;
        this.metadata.putString("id", ServuxLitematicaPacket.CHANNEL.toString());
        this.metadata.putInt("version", ServuxLitematicaPacket.PROTOCOL_VERSION);
        this.metadata.putInt("timeout", 30 * 20);
        this.loadPermissionSettings();
    }

    private void loadPermissionSettings()
    {
        Path path = FabricLoader.getInstance().getConfigDir().resolve("servux.json");

        if (Files.exists(path) == false)
        {
            return;
        }

        try (Reader reader = Files.newBufferedReader(path))
        {
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();

            if (root.has("litematic_data") && root.get("litematic_data").isJsonObject())
            {
                JsonObject data = root.getAsJsonObject("litematic_data");

                if (data.has("permission_level"))
                {
                    this.permissionLevel = Math.max(0, Math.min(4, data.get("permission_level").getAsInt()));
                }
                if (data.has("permission_level_paste"))
                {
                    this.pastePermissionLevel = Math.max(0, Math.min(4, data.get("permission_level_paste").getAsInt()));
                }
            }
        }
        catch (Exception e)
        {
            Servux.LOGGER.warn("Failed to read litematic_data permission levels from servux.json: {}", e.getMessage());
        }
    }

    public int getPermissionLevel()
    {
        return this.permissionLevel;
    }

    public int getPastePermissionLevel()
    {
        return this.pastePermissionLevel;
    }

    public void setPermissionLevel(int level)
    {
        this.permissionLevel = Math.max(0, Math.min(4, level));
    }

    public void setPastePermissionLevel(int level)
    {
        this.pastePermissionLevel = Math.max(0, Math.min(4, level));
    }

    public boolean hasPermission(ServerPlayerEntity player)
    {
        return Permissions.check(player, this.permNode, this.permissionLevel);
    }

    public boolean hasPermissionsForPaste(ServerPlayerEntity player)
    {
        return this.hasPermission(player) &&
               Permissions.check(player, this.permNode + ".paste", this.pastePermissionLevel);
    }

    @Override
    public IPluginChannelHandler getPacketHandler()
    {
        return ServuxLitematicaHandler.INSTANCE;
    }

    public SchematicBufferManager getBufferManager()
    {
        return this.bufferManager;
    }

    public Path getTransmitDir()
    {
        if (this.transmitDir == null)
        {
            this.transmitDir = FabricLoader.getInstance().getConfigDir().resolve("servux").resolve("litematics");

            try
            {
                Files.createDirectories(this.transmitDir);
            }
            catch (IOException e)
            {
                Servux.LOGGER.error("Failed to create the Litematica transmit directory '{}': {}",
                                    this.transmitDir, e.getLocalizedMessage());
            }
        }

        return this.transmitDir;
    }

    public void sendMetadata(ServerPlayerEntity player)
    {
        if (this.hasPermission(player) == false)
        {
            return;
        }

        ServuxLitematicaHandler.INSTANCE.send(player, ServuxLitematicaPacket.MetadataResponse(this.metadata));
    }

    public void handlePasteRequest(ServerPlayerEntity player, NbtCompound tags)
    {
        if (this.isEnabled() == false)
        {
            return;
        }

        if ("LitematicaPaste".equals(tags.getString("Task")) == false)
        {
            Servux.debugLog("litematic_data: ignoring unknown task '{}' from {}", tags.getString("Task"), player.getName().getString());
            return;
        }

        if (this.hasPermissionsForPaste(player) == false)
        {
            Servux.debugLog("litematic_data: Denying paste for {}, permission level {}",
                            player.getName().getString(), this.pastePermissionLevel);
            player.sendMessage(Text.translatable("servux.litematics.error.insufficent_for_paste"), false);
            return;
        }

        if (player.isCreative() == false)
        {
            player.sendMessage(Text.translatable("servux.litematics.error.creative_required"), false);
            return;
        }

        try
        {
            long timeStart = System.currentTimeMillis();
            SchematicPlacement placement = SchematicPlacement.createFromNbt(tags);

            if (placement == null)
            {
                player.sendMessage(Text.translatable("servux.litematics.error.placement_invalid"), false);
                return;
            }

            ReplaceBehavior replaceMode = ReplaceBehavior.fromStringStatic(tags.getString("ReplaceMode"));
            PasteLayerBehavior layerBehavior = PasteLayerBehavior.fromStringStatic(tags.getString("PasteLayerBehavior"));

            // Always use the full 1.21 placement paste path. The old pasteDirect()
            // path ignored placement rotation/mirror and sub-region offsets, which
            // caused builds to be pasted mirrored or shifted.
            Servux.LOGGER.info("litematic_data: paste origin={} rotation={} mirror={} modified={} regions={}",
                               placement.getOrigin(), placement.getRotation(), placement.getMirror(),
                               placement.isRegionPlacementModified(),
                               placement.getSchematic().getAreaPositions().size());
            placement.pasteTo(player.getServerWorld(), replaceMode, layerBehavior, null);

            long timeElapsed = System.currentTimeMillis() - timeStart;
            String worldId = player.getServerWorld().getRegistryKey().getValue().toString();
            player.sendMessage(Text.translatable("servux.litematics.success.pasted",
                                                 placement.getName(), worldId, timeElapsed), false);
            ServuxLitematicaHandler.INSTANCE.send(player, ServuxLitematicaPacket.PasteResult(true, "ok"));
        }
        catch (Exception e)
        {
            Servux.LOGGER.error("litematic_data: exception while pasting for {}: {}", player.getName().getString(), e.getLocalizedMessage());
            player.sendMessage(Text.translatable("servux.litematics.error.paste_failed", e.getMessage()), false);
            ServuxLitematicaHandler.INSTANCE.send(player, ServuxLitematicaPacket.PasteResult(false, e.getMessage()));
        }
    }

    /**
     * Straight-forward 1.20.1 paste implementation: copies the block states of all
     * schematic regions to the world, relative to the placement origin.
     */
    private int pasteDirect(net.minecraft.server.world.ServerWorld world, SchematicPlacement placement, ReplaceBehavior replaceMode)
    {
        fi.dy.masa.servux.schematic.LitematicaSchematic schematic = placement.getSchematic();
        net.minecraft.util.math.BlockPos origin = placement.getOrigin();
        int placed = 0;

        for (String region : schematic.getAreaPositions().keySet())
        {
            net.minecraft.util.math.BlockPos regionPos = schematic.getSubRegionPosition(region);
            fi.dy.masa.servux.schematic.container.LitematicaBlockStateContainer container = schematic.getSubRegionContainer(region);

            if (regionPos == null || container == null)
            {
                continue;
            }

            net.minecraft.util.math.Vec3i size = container.getSize();

            for (int y = 0; y < size.getY(); ++y)
            {
                for (int z = 0; z < size.getZ(); ++z)
                {
                    for (int x = 0; x < size.getX(); ++x)
                    {
                        net.minecraft.block.BlockState state = container.get(x, y, z);

                        if (state.isAir())
                        {
                            continue;
                        }

                        net.minecraft.util.math.BlockPos pos = origin.add(regionPos).add(x, y, z);
                        net.minecraft.block.BlockState current = world.getBlockState(pos);

                        if (replaceMode == ReplaceBehavior.NONE && current.isAir() == false)
                        {
                            continue;
                        }
                        else if (replaceMode == ReplaceBehavior.WITH_NON_AIR && current.isAir())
                        {
                            continue;
                        }

                        world.setBlockState(pos, state, 2);
                        ++placed;
                    }
                }
            }
        }

        Servux.LOGGER.info("litematic_data: pasted {} blocks directly", placed);
        return placed;
    }
}
