package com.sk89q.worldedit.pnx;

import cn.nukkit.Player;
import cn.nukkit.item.Item;
import com.fastasyncworldedit.core.extent.inventory.SlottableBlockBag;
import com.fastasyncworldedit.core.util.TaskManager;
import com.sk89q.worldedit.blocks.BaseItem;
import com.sk89q.worldedit.blocks.BaseItemStack;
import com.sk89q.worldedit.extent.inventory.BlockBag;
import com.sk89q.worldedit.extent.inventory.BlockBagException;
import com.sk89q.worldedit.extent.inventory.OutOfBlocksException;
import com.sk89q.worldedit.extent.inventory.OutOfSpaceException;
import com.sk89q.worldedit.util.Location;
import com.sk89q.worldedit.world.block.BlockState;

import java.util.Map;

//FAWE start - implements SlottableBlockBag
public class PNXPlayerBlockBag extends BlockBag implements SlottableBlockBag {
//FAWE end

    private final Player player;
    private Map<Integer, Item> items;

    /**
     * Construct the object.
     *
     * @param player the player
     */
    public PNXPlayerBlockBag(Player player) {
        this.player = player;
    }

    /**
     * Loads inventory on first use.
     */
    private void loadInventory() {
        if (items == null) {
            items = player.getInventory().getContents();
        }
    }

    /**
     * Get the player.
     *
     * @return the player
     */
    public Player getPlayer() {
        return player;
    }

    @Override
    public void fetchBlock(BlockState blockState) throws BlockBagException {
        if (blockState.getBlockType().getMaterial().isAir()) {
            throw new IllegalArgumentException("Can't fetch air block");
        }

        loadInventory();

        boolean found = false;

        for (var entry : items.entrySet()) {
            Item item = entry.getValue();

            if (item == null) {
                continue;
            }

            if (!PNXAdapter.equals(blockState.getBlockType(), item)) {
                // Type id doesn't fit
                continue;
            }

            int currentAmount = item.getCount();
            if (currentAmount < 0) {
                // Unlimited
                return;
            }

            if (currentAmount > 1) {
                item.setCount(currentAmount - 1);
                found = true;
            } else {
                items.put(entry.getKey(), null);
                found = true;
            }

            break;
        }

        if (!found) {
            throw new OutOfBlocksException();
        }
    }

    @Override
    public void storeBlock(BlockState blockState, int amount) throws BlockBagException {
        if (blockState.getBlockType().getMaterial().isAir()) {
            throw new IllegalArgumentException("Can't store air block");
        }
        if (!blockState.getBlockType().hasItemType()) {
            throw new IllegalArgumentException("This block cannot be stored");
        }

        loadInventory();

        int freeSlot = -1;

        for (var entry : items.entrySet()) {
            Item item = entry.getValue();

            if (item == null) {
                // Delay using up a free slot until we know there are no stacks
                // of this item to merge into

                if (freeSlot == -1) {
                    freeSlot = entry.getKey();
                }
                continue;
            }

            if (!PNXAdapter.equals(blockState.getBlockType(), item)) {
                // Type id doesn't fit
                continue;
            }

            int currentAmount = item.getCount();
            if (currentAmount < 0) {
                // Unlimited
                return;
            }
            if (currentAmount >= 64) {
                // Full stack
                continue;
            }

            int spaceLeft = 64 - currentAmount;
            if (spaceLeft >= amount) {
                item.setCount(currentAmount + amount);
                return;
            }

            item.setCount(64);
            amount -= spaceLeft;
        }

        if (freeSlot > -1) {
            items.put(freeSlot, PNXAdapter.adapt(new BaseItemStack(blockState.getBlockType().getItemType(), amount)));
            return;
        }

        throw new OutOfSpaceException(blockState.getBlockType());
    }

    @Override
    public void flushChanges() {
        if (items != null) {
            TaskManager.taskManager().sync(() -> {
                player.getInventory().setContents(items);
                return null;
            });
            items = null;
        }
    }

    @Override
    public void addSourcePosition(Location pos) {
    }

    @Override
    public void addSingleSourcePosition(Location pos) {
    }

    //FAWE start
    @Override
    public BaseItem getItem(int slot) {
        loadInventory();
        return PNXAdapter.adapt(items.get(slot));
    }

    @Override
    public void setItem(int slot, BaseItem block) {
        loadInventory();
        BaseItemStack stack = block instanceof BaseItemStack ? (BaseItemStack) block : new BaseItemStack(
                block.getType(),
                block.getNbtData(),
                1
        );
        items.put(slot, PNXAdapter.adapt(stack));
    }
    //FAWE end

}
