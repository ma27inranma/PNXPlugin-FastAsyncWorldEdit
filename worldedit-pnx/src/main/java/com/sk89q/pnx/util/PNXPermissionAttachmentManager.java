package com.sk89q.pnx.util;

import cn.nukkit.Player;
import cn.nukkit.permission.PermissibleBase;
import cn.nukkit.permission.PermissionAttachment;
import com.sk89q.worldedit.pnx.PNXWorldEditPlugin;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class PNXPermissionAttachmentManager {

    private final PNXWorldEditPlugin plugin;
    private final Map<Player, PermissionAttachment> attachments = new ConcurrentHashMap<>();
    private PermissionAttachment noopAttachment;

    public PNXPermissionAttachmentManager(PNXWorldEditPlugin plugin) {
        this.plugin = plugin;
    }

    public PermissionAttachment getOrAddAttachment(@Nullable final Player p) {
        if (p == null) {
            return null;
        }
        if (p.hasMetadata("NPC")) {
            if (this.noopAttachment == null) {
                this.noopAttachment = new PermissionAttachment(plugin, new PermissibleBase(null));
            }
            return noopAttachment;
        }
        return attachments.computeIfAbsent(p, k -> k.addAttachment(plugin));
    }

    public void removeAttachment(@Nullable final Player p) {
        if (p == null) {
            return;
        }
        PermissionAttachment attach = attachments.remove(p);
        if (attach != null) {
            p.removeAttachment(attach);
        }
    }

}
