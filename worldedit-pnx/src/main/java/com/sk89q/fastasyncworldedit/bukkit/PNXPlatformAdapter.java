package com.sk89q.fastasyncworldedit.bukkit;

import cn.nukkit.Player;
import cn.nukkit.level.Position;
import com.fastasyncworldedit.core.FAWEPlatformAdapterImpl;
import com.fastasyncworldedit.core.queue.IChunkGet;
import com.sk89q.worldedit.pnx.PNXGetBlocks;
import com.sk89q.worldedit.pnx.PNXWorldEditPlugin;

import java.util.Collection;

public class PNXPlatformAdapter implements FAWEPlatformAdapterImpl {

    public static final PNXPlatformAdapter INSTANCE = new PNXPlatformAdapter();

    private PNXPlatformAdapter() {
    }

    @Override
    public void sendChunk(final IChunkGet chunk, final int mask, final boolean lighting) {
        if (chunk instanceof PNXGetBlocks pnxGetBlocks) {
            Collection<Player> players = PNXWorldEditPlugin.getInstance().getServer().getOnlinePlayers().values();
            int view = PNXWorldEditPlugin.getInstance().getServer().getViewDistance();
            for (Player player : players) {
                Position pos = player.getPosition();
                int pcx = pos.getFloorX() >> 4;
                int pcz = pos.getFloorZ() >> 4;
                if (Math.abs(pcx - pnxGetBlocks.getChunkX()) > view || Math.abs(pcz - pnxGetBlocks.getChunkZ()) > view) {
                    continue;
                }
                pnxGetBlocks.getServerLevel().requestChunk(pnxGetBlocks.getChunkX(), pnxGetBlocks.getChunkZ(), player);
            }
        }
    }

}
