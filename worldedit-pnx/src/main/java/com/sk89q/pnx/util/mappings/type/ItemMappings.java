package com.sk89q.pnx.util.mappings.type;


import cn.nukkit.item.Item;
import com.google.common.collect.HashBiMap;
import com.sk89q.worldedit.world.item.ItemType;
import lombok.Builder;
import lombok.Value;

import java.util.Map;
import java.util.Objects;

@Builder
@Value
public class ItemMappings {

    HashBiMap<HashItem, ItemType> mapping;
    Map<String, Byte> itemDamageMapping;


    public static class HashItem {

        private final Item item;

        private HashItem(Item item) {
            this.item = item;
        }

        public static HashItem of(Item item) {
            return new HashItem(item);
        }

        public Item getItem() {
            return item;
        }

        @Override
        public int hashCode() {
            return Objects.hash(this.item.getId(), this.item.getDamage());
        }

        @Override
        public boolean equals(final Object obj) {
            if (obj instanceof HashItem hashItem) {
                return this.equals(hashItem);
            } else {
                return false;
            }
        }

        public boolean equals(final HashItem obj) {
            return this.item.getId().equals(obj.item.getId()) && this.item.getDamage() == obj.item.getDamage();
        }

        @Override
        public String toString() {
            return this.item.getId() + ":" + this.item.getDamage();
        }

    }

}
