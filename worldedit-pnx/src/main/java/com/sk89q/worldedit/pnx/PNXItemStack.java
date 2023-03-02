package com.sk89q.worldedit.pnx;

import cn.nukkit.item.Item;
import com.sk89q.worldedit.blocks.BaseItemStack;
import com.sk89q.worldedit.util.concurrency.LazyReference;
import com.sk89q.worldedit.util.nbt.CompoundBinaryTag;
import com.sk89q.worldedit.world.item.ItemType;
import org.jetbrains.annotations.Nullable;

public class PNXItemStack extends BaseItemStack {

    private Item stack;
    private boolean loadedNBT;

    public PNXItemStack(Item stack) {
        super(PNXAdapter.asItemType(stack));
        this.stack = stack;
    }

    public PNXItemStack(ItemType type, Item stack) {
        super(type);
        this.stack = stack;
    }

    @Override
    public int getAmount() {
        return stack.getCount();
    }

    @Override
    public void setAmount(final int amount) {
        this.stack.setCount(amount);
    }

    public Item getPNXItem() {
        return stack;
    }

    @Override
    public void setNbtReference(@Nullable final LazyReference<CompoundBinaryTag> nbtData) {
        if (nbtData != null) {
            this.stack.setNamedTag(NBTConverter.toNativeLazy(nbtData.getValue()));
        }
    }

    @Nullable
    @Override
    public LazyReference<CompoundBinaryTag> getNbtReference() {
        return LazyReference.computed(NBTConverter.fromNative(this.stack.getNamedTag()));
    }

}
