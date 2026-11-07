package fi.dy.masa.servux.util;

import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;

public class InventoryUtils
{
    public static boolean isShulkerBox(ItemStack stack)
    {
        return stack.getItem() instanceof BlockItem blockItem && blockItem.getBlock() instanceof ShulkerBoxBlock;
    }

    public static boolean shulkerBoxHasItems(ItemStack stack)
    {
        NbtCompound nbt = stack.getNbt();

        if (nbt != null && nbt.contains("BlockEntityTag", 10))
        {
            NbtCompound blockEntityTag = nbt.getCompound("BlockEntityTag");

            if (blockEntityTag.contains("Items", 9))
            {
                NbtList items = blockEntityTag.getList("Items", 10);
                return items.isEmpty() == false;
            }
        }

        return false;
    }
}
