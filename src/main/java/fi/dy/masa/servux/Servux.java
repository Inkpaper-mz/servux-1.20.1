package fi.dy.masa.servux;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import fi.dy.masa.servux.dataproviders.DataProviderManager;
import fi.dy.masa.servux.dataproviders.StructureDataProvider;
import fi.dy.masa.servux.dataproviders.LitematicsDataProvider;
import fi.dy.masa.servux.dataproviders.ServuxConfigProvider;
import fi.dy.masa.servux.network.ServerPacketChannelHandler;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;

public class Servux implements ModInitializer
{
    public static final Logger logger = LogManager.getLogger(Reference.MOD_ID);
    public static final Logger LOGGER = logger;

    @Override
    public void onInitialize()
    {
        DataProviderManager.INSTANCE.registerDataProvider(StructureDataProvider.INSTANCE);
        DataProviderManager.INSTANCE.registerDataProvider(LitematicsDataProvider.INSTANCE);
        ServerPacketChannelHandler.INSTANCE.registerServerChannelHandler(LitematicsDataProvider.INSTANCE.getPacketHandler());
        DataProviderManager.INSTANCE.readFromConfig();

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            if (handler.player != null)
            {
                LitematicsDataProvider.INSTANCE.sendMetadata(handler.player);
            }
        });
    }

    public static void debugLog(String msg, Object... args)
    {
        if (ServuxConfigProvider.INSTANCE.hasDebugMode())
        {
            logger.info(msg, args);
        }
    }

    public static String getModVersionString(String modId)
    {
        for (net.fabricmc.loader.api.ModContainer container : net.fabricmc.loader.api.FabricLoader.getInstance().getAllMods())
        {
            if (container.getMetadata().getId().equals(modId))
            {
                return container.getMetadata().getVersion().getFriendlyString();
            }
        }

        return "?";
    }
}
