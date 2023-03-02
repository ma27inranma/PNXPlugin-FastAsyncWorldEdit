/*
 * WorldEdit, a Minecraft world manipulation toolkit
 * Copyright (C) sk89q <http://www.sk89q.com>
 * Copyright (C) WorldEdit team and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.sk89q.pnx.util;

import cn.nukkit.Server;
import cn.nukkit.command.Command;
import cn.nukkit.command.CommandExecutor;
import cn.nukkit.command.CommandMap;
import cn.nukkit.command.PluginIdentifiableCommand;
import cn.nukkit.command.SimpleCommandMap;
import cn.nukkit.plugin.Plugin;
import com.google.common.collect.Sets;
import com.sk89q.util.ReflectionUtil;
import com.sk89q.worldedit.pnx.PNXWorldEditPlugin;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class CommandRegistration {

    protected final Plugin plugin;
    protected final CommandExecutor executor;
    private CommandMap serverCommandMap;
    private CommandMap fallbackCommands;

    public CommandRegistration(Plugin plugin) {
        this(plugin, plugin);
    }

    public CommandRegistration(Plugin plugin, CommandExecutor executor) {
        this.plugin = plugin;
        this.executor = executor;
    }

    public Plugin getCommandOwner(String label) {
        if (serverCommandMap == null) {
            return null;
        }
        Command command = serverCommandMap.getCommand(label);
        if (command instanceof PluginIdentifiableCommand) {
            return ((PluginIdentifiableCommand) command).getPlugin();
        }
        return null;
    }

    public boolean register(List<CommandInfo> registered) {
        CommandMap commandMap = getCommandMap();
        if (registered == null || commandMap == null) {
            return false;
        }
        for (CommandInfo command : registered) {
            DynamicPluginCommand cmd = new DynamicPluginCommand(
                    command.getAliases(),
                    command.getDesc(),
                    command.getUsage(),
                    executor,
                    command.getRegisteredWith(),
                    plugin
            );
            cmd.setPermissions(command.getPermissions());
            commandMap.register(plugin.getDescription().getName(), cmd);
        }
        return true;
    }

    public CommandMap getCommandMap() {
        if (serverCommandMap != null) {
            return serverCommandMap;
        }
        if (fallbackCommands != null) {
            return fallbackCommands;
        }

        CommandMap commandMap = ReflectionUtil.getField(plugin.getServer().getPluginManager(), "commandMap");
        if (commandMap == null) {
            PNXWorldEditPlugin.getInstance().getLogger().error(plugin.getDescription().getName()
                    + ": Could not retrieve server CommandMap, using fallback instead!");
            fallbackCommands = commandMap = new SimpleCommandMap(Server.getInstance());
        } else {
            serverCommandMap = commandMap;
        }
        return commandMap;
    }

    public boolean unregisterCommands() {
        CommandMap commandMap = getCommandMap();
        List<String> toRemove = new ArrayList<>();
        Map<String, Command> knownCommands = ReflectionUtil.getField(commandMap, "knownCommands");

        if (knownCommands == null) {
            return false;
        }
        for (Iterator<Command> i = knownCommands.values().iterator(); i.hasNext(); ) {
            Command cmd = i.next();
            if (cmd instanceof DynamicPluginCommand && ((DynamicPluginCommand) cmd).getOwner().equals(executor)) {
                i.remove();
                for (String alias : cmd.getAliases()) {
                    Command aliasCmd = knownCommands.get(alias);
                    if (cmd.equals(aliasCmd)) {
                        var set = Sets.newHashSet(cmd.getAliases());
                        set.remove(alias);
                        cmd.setAliases(set.toArray(new String[]{}));
                        toRemove.add(alias);
                    }
                }
            }
        }
        for (String string : toRemove) {
            knownCommands.remove(string);
        }
        return true;
    }

}
