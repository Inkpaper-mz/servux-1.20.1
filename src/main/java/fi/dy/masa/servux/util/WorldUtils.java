package fi.dy.masa.servux.util;

import net.minecraft.world.World;

/**
 * 1.20.1 port: the world update suppression mixins are not ported, so these are no-ops.
 */
public class WorldUtils
{
    public static boolean getShouldPreventBlockUpdates(World world)
    {
        return false;
    }

    public static void setShouldPreventBlockUpdates(World world, boolean preventUpdates)
    {
    }
}
