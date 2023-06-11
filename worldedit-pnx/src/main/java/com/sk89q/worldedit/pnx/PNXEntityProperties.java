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

package com.sk89q.worldedit.pnx;

import cn.nukkit.Player;
import cn.nukkit.blockentity.BlockEntity;
import cn.nukkit.blockentity.BlockEntityBanner;
import cn.nukkit.blockentity.BlockEntityItemFrame;
import cn.nukkit.entity.Entity;
import cn.nukkit.entity.EntityLiving;
import cn.nukkit.entity.EntityOwnable;
import cn.nukkit.entity.EntitySwimmable;
import cn.nukkit.entity.IHuman;
import cn.nukkit.entity.item.EntityArmorStand;
import cn.nukkit.entity.item.EntityBoat;
import cn.nukkit.entity.item.EntityFallingBlock;
import cn.nukkit.entity.item.EntityItem;
import cn.nukkit.entity.item.EntityMinecartAbstract;
import cn.nukkit.entity.item.EntityPrimedTNT;
import cn.nukkit.entity.item.EntityXPOrb;
import cn.nukkit.entity.mob.EntityIronGolem;
import cn.nukkit.entity.passive.EntityAnimal;
import cn.nukkit.entity.passive.EntityNPC;
import cn.nukkit.entity.projectile.EntityProjectile;
import com.sk89q.worldedit.entity.metadata.EntityProperties;

import static com.google.common.base.Preconditions.checkNotNull;

class PNXEntityProperties implements EntityProperties {

    protected final Entity entity;
    protected final BlockEntity blockEntity;

    PNXEntityProperties(Entity entity) {
        checkNotNull(entity);
        this.entity = entity;
        this.blockEntity = null;
    }

    PNXEntityProperties(BlockEntity entity) {
        checkNotNull(entity);
        this.entity = null;
        this.blockEntity = entity;
    }

    @Override
    public boolean isPlayerDerived() {
        if (entity != null) {
            return entity instanceof IHuman;
        } else {
            return false;
        }
    }

    @Override
    public boolean isProjectile() {
        if (entity != null) {
            return entity instanceof EntityProjectile;
        } else {
            return false;
        }

    }

    @Override
    public boolean isItem() {
        if (entity != null) {
            return entity instanceof EntityItem;
        } else {
            return false;
        }

    }

    @Override
    public boolean isFallingBlock() {
        if (entity != null) {
            return entity instanceof EntityFallingBlock;
        } else {
            return false;
        }

    }

    @Override
    public boolean isPainting() {
        if (blockEntity != null) {
            return blockEntity instanceof BlockEntityBanner;
        } else {
            return false;
        }
    }

    @Override
    public boolean isItemFrame() {
        if (blockEntity != null) {
            return blockEntity instanceof BlockEntityItemFrame;
        } else {
            return false;
        }
    }

    @Override
    public boolean isBoat() {
        if (entity != null) {
            return entity instanceof EntityBoat;
        } else {
            return false;
        }
    }

    @Override
    public boolean isMinecart() {
        if (entity != null) {
            return entity instanceof EntityMinecartAbstract;
        } else {
            return false;
        }
    }

    @Override
    public boolean isTNT() {
        if (entity != null) {
            return entity instanceof EntityPrimedTNT;
        } else {
            return false;
        }
    }

    @Override
    public boolean isExperienceOrb() {
        if (entity != null) {
            return entity instanceof EntityXPOrb;
        } else {
            return false;
        }
    }

    @Override
    public boolean isLiving() {
        if (entity != null) {
            return entity instanceof EntityLiving;
        } else {
            return false;
        }
    }

    @Override
    public boolean isAnimal() {
        if (entity != null) {
            return entity instanceof EntityAnimal;
        } else {
            return false;
        }
    }

    @Override
    public boolean isAmbient() {
        if (entity != null) {
            return entity instanceof EntityAnimal;
        } else {
            return false;
        }
    }

    @Override
    public boolean isNPC() {
        if (entity != null) {
            return entity instanceof EntityNPC;
        } else {
            return false;
        }
    }

    @Override
    public boolean isGolem() {
        if (entity != null) {
            return entity instanceof EntityIronGolem;
        } else {
            return false;
        }
    }

    @Override
    public boolean isTamed() {
        if (entity != null) {
            return entity instanceof EntityOwnable;
        } else {
            return false;
        }
    }

    @Override
    public boolean isTagged() {
        if (entity != null) {
            return entity instanceof EntityLiving && entity.hasCustomName();
        } else {
            return false;
        }
    }

    @Override
    public boolean isArmorStand() {
        if (entity != null) {
            return entity instanceof EntityArmorStand;
        } else {
            return false;
        }
    }

    @Override
    public boolean isPasteable() {
        if (entity != null) {
            return !(entity instanceof Player);
        } else {
            return false;
        }
    }

    @Override
    public boolean isWaterCreature() {
        if (entity != null) {
            return entity instanceof EntitySwimmable;
        } else {
            return false;
        }
    }

}
