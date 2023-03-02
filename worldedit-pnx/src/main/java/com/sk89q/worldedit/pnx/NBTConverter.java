package com.sk89q.worldedit.pnx;

import cn.nukkit.nbt.tag.ByteArrayTag;
import cn.nukkit.nbt.tag.ByteTag;
import cn.nukkit.nbt.tag.CompoundTag;
import cn.nukkit.nbt.tag.DoubleTag;
import cn.nukkit.nbt.tag.EndTag;
import cn.nukkit.nbt.tag.FloatTag;
import cn.nukkit.nbt.tag.IntArrayTag;
import cn.nukkit.nbt.tag.IntTag;
import cn.nukkit.nbt.tag.ListTag;
import cn.nukkit.nbt.tag.LongTag;
import cn.nukkit.nbt.tag.ShortTag;
import cn.nukkit.nbt.tag.StringTag;
import cn.nukkit.nbt.tag.Tag;
import com.google.common.collect.ImmutableMap;
import com.sk89q.worldedit.util.nbt.BinaryTag;
import com.sk89q.worldedit.util.nbt.BinaryTagType;
import com.sk89q.worldedit.util.nbt.ByteArrayBinaryTag;
import com.sk89q.worldedit.util.nbt.ByteBinaryTag;
import com.sk89q.worldedit.util.nbt.CompoundBinaryTag;
import com.sk89q.worldedit.util.nbt.DoubleBinaryTag;
import com.sk89q.worldedit.util.nbt.EndBinaryTag;
import com.sk89q.worldedit.util.nbt.FloatBinaryTag;
import com.sk89q.worldedit.util.nbt.IntArrayBinaryTag;
import com.sk89q.worldedit.util.nbt.IntBinaryTag;
import com.sk89q.worldedit.util.nbt.ListBinaryTag;
import com.sk89q.worldedit.util.nbt.LongBinaryTag;
import com.sk89q.worldedit.util.nbt.ShortBinaryTag;
import com.sk89q.worldedit.util.nbt.StringBinaryTag;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Converts between Jcn.nukkit.nbt.tag. and Minecraft cn.nukkit.nbt.tag. classes.
 */
public final class NBTConverter {

    private static Field tagsField;

    private NBTConverter() {
    }

    static {
        try {
            tagsField = cn.nukkit.nbt.tag.CompoundTag.class.getDeclaredField("tags");
            tagsField.setAccessible(true);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    public static Map<String, Tag> getMap(cn.nukkit.nbt.tag.CompoundTag other) {
        try {
            return (Map<String, cn.nukkit.nbt.tag.Tag>) tagsField.get(other);
        } catch (Throwable e) {
            e.printStackTrace();
            return null;
        }
    }

    @SuppressWarnings("rawtypes")
    public static CompoundTag fromNativeLazy(cn.nukkit.nbt.tag.CompoundTag other) {
        try {
            Map tags = (Map) tagsField.get(other);
            CompoundTag ct = new CompoundTag(tags);
            return ct;
        } catch (Throwable e) {
            e.printStackTrace();
            return null;
        }
    }

    public static cn.nukkit.nbt.tag.CompoundTag toNativeLazy(CompoundBinaryTag tag) {
        try {
            ImmutableMap.Builder<String, cn.nukkit.nbt.tag.Tag> map = ImmutableMap.builder();
            for (String key : tag.keySet()) {
                map.put(key, toNative(tag.get(key)));
            }
            cn.nukkit.nbt.tag.CompoundTag ct = new cn.nukkit.nbt.tag.CompoundTag();
            tagsField.set(ct, map);
            return ct;
        } catch (Throwable e) {
            e.printStackTrace();
            return null;
        }
    }

    public static CompoundBinaryTag fromNative(cn.nukkit.nbt.tag.CompoundTag other) {
        Map<String, cn.nukkit.nbt.tag.Tag> tags = other.getTags();
        Map<String, BinaryTag> map = new HashMap<String, BinaryTag>();
        for (Map.Entry<String, Tag> entry : tags.entrySet()) {
            map.put(entry.getKey(), fromNative(entry.getValue()));
        }
        return CompoundBinaryTag.from(map);
    }

    public static cn.nukkit.nbt.tag.Tag toNative(BinaryTag tag) {
        if (tag instanceof IntArrayTag) {
            return toNative((IntArrayBinaryTag) tag);

        } else if (tag instanceof ListTag) {
            return toNative((ListBinaryTag) tag);

        } else if (tag instanceof LongTag) {
            return toNative((LongBinaryTag) tag);

        } else if (tag instanceof StringTag) {
            return toNative((StringBinaryTag) tag);

        } else if (tag instanceof IntTag) {
            return toNative((IntBinaryTag) tag);

        } else if (tag instanceof ByteTag) {
            return toNative((ByteBinaryTag) tag);

        } else if (tag instanceof ByteArrayTag) {
            return toNative((ByteArrayBinaryTag) tag);

        } else if (tag instanceof CompoundTag) {
            return toNative((CompoundBinaryTag) tag);

        } else if (tag instanceof FloatTag) {
            return toNative((FloatBinaryTag) tag);

        } else if (tag instanceof ShortTag) {
            return toNative((ShortBinaryTag) tag);

        } else if (tag instanceof DoubleTag) {
            return toNative((DoubleBinaryTag) tag);
        } else {
            throw new IllegalArgumentException("Can't convert tag of type " + tag.getClass().getCanonicalName());
        }
    }

    private static cn.nukkit.nbt.tag.IntArrayTag toNative(IntArrayBinaryTag tag) {
        int[] value = tag.value();
        return new cn.nukkit.nbt.tag.IntArrayTag("", Arrays.copyOf(value, value.length));
    }

    @SuppressWarnings("rawtypes")
    private static cn.nukkit.nbt.tag.ListTag toNative(ListBinaryTag tag) {
        cn.nukkit.nbt.tag.ListTag list = new cn.nukkit.nbt.tag.ListTag();
        for (final BinaryTag child : tag) {
            if (child instanceof EndTag) {
                continue;
            }
            list.add(toNative(child));
        }
        return list;
    }

    private static cn.nukkit.nbt.tag.LongTag toNative(LongBinaryTag tag) {
        return new cn.nukkit.nbt.tag.LongTag("", tag.value());
    }

    private static cn.nukkit.nbt.tag.StringTag toNative(StringBinaryTag tag) {
        return new cn.nukkit.nbt.tag.StringTag("", tag.value());
    }

    private static cn.nukkit.nbt.tag.IntTag toNative(IntBinaryTag tag) {
        return new cn.nukkit.nbt.tag.IntTag("", tag.value());
    }

    private static cn.nukkit.nbt.tag.ByteTag toNative(ByteBinaryTag tag) {
        return new cn.nukkit.nbt.tag.ByteTag("", tag.value());
    }

    private static cn.nukkit.nbt.tag.ByteArrayTag toNative(ByteArrayBinaryTag tag) {
        byte[] value = tag.value();
        return new cn.nukkit.nbt.tag.ByteArrayTag("", Arrays.copyOf(value, value.length));
    }

    private static cn.nukkit.nbt.tag.CompoundTag toNative(CompoundBinaryTag tag) {
        cn.nukkit.nbt.tag.CompoundTag compound = new cn.nukkit.nbt.tag.CompoundTag();
        for (final Map.Entry<String, ? extends BinaryTag> entry : tag) {
            Tag value = toNative(entry.getValue());
            value.setName(entry.getKey());
            compound.put(entry.getKey(), value);
        }
        return compound;
    }

    private static cn.nukkit.nbt.tag.FloatTag toNative(FloatBinaryTag tag) {
        return new cn.nukkit.nbt.tag.FloatTag("", tag.value());
    }

    private static cn.nukkit.nbt.tag.ShortTag toNative(ShortBinaryTag tag) {
        return new cn.nukkit.nbt.tag.ShortTag("", tag.value());
    }

    private static cn.nukkit.nbt.tag.DoubleTag toNative(DoubleBinaryTag tag) {
        return new cn.nukkit.nbt.tag.DoubleTag("", tag.value());
    }

    @SuppressWarnings("rawtypes")
    private static BinaryTag fromNative(cn.nukkit.nbt.tag.Tag other) {
        if (other instanceof cn.nukkit.nbt.tag.IntArrayTag) {
            return fromNative((cn.nukkit.nbt.tag.IntArrayTag) other);

        } else if (other instanceof cn.nukkit.nbt.tag.ListTag) {
            return fromNative((cn.nukkit.nbt.tag.ListTag) other);

        } else if (other instanceof cn.nukkit.nbt.tag.EndTag) {
            return fromNative((cn.nukkit.nbt.tag.EndTag) other);

        } else if (other instanceof cn.nukkit.nbt.tag.LongTag) {
            return fromNative((cn.nukkit.nbt.tag.LongTag) other);

        } else if (other instanceof cn.nukkit.nbt.tag.StringTag) {
            return fromNative((cn.nukkit.nbt.tag.StringTag) other);

        } else if (other instanceof cn.nukkit.nbt.tag.IntTag) {
            return fromNative((cn.nukkit.nbt.tag.IntTag) other);

        } else if (other instanceof cn.nukkit.nbt.tag.ByteTag) {
            return fromNative((cn.nukkit.nbt.tag.ByteTag) other);

        } else if (other instanceof cn.nukkit.nbt.tag.ByteArrayTag) {
            return fromNative((cn.nukkit.nbt.tag.ByteArrayTag) other);

        } else if (other instanceof cn.nukkit.nbt.tag.CompoundTag) {
            return fromNative((cn.nukkit.nbt.tag.CompoundTag) other);

        } else if (other instanceof cn.nukkit.nbt.tag.FloatTag) {
            return fromNative((cn.nukkit.nbt.tag.FloatTag) other);

        } else if (other instanceof cn.nukkit.nbt.tag.ShortTag) {
            return fromNative((cn.nukkit.nbt.tag.ShortTag) other);

        } else if (other instanceof cn.nukkit.nbt.tag.DoubleTag) {
            return fromNative((cn.nukkit.nbt.tag.DoubleTag) other);
        } else {
            throw new IllegalArgumentException("Can't convert other of type " + other.getClass().getCanonicalName());
        }
    }

    private static IntArrayBinaryTag fromNative(cn.nukkit.nbt.tag.IntArrayTag other) {
        int[] value = other.data;
        return IntArrayBinaryTag.of(Arrays.copyOf(value, value.length));
    }

    @SuppressWarnings("rawtypes")
    private static ListBinaryTag fromNative(cn.nukkit.nbt.tag.ListTag other) {
        other = (cn.nukkit.nbt.tag.ListTag) other.copy();
        List<BinaryTag> list = new ArrayList<>();
        BinaryTagType<? extends BinaryTag> listClass = StringBinaryTag.of("").type();
        int tags = other.size();
        for (int i = 0; i < tags; i++) {
            BinaryTag child = fromNative(other.get(0));
            other.remove(0);
            list.add(child);
            listClass = child.type();
        }
        return ListBinaryTag.of(listClass, list);
    }

    private static EndBinaryTag fromNative(cn.nukkit.nbt.tag.EndTag other) {
        return EndBinaryTag.get();
    }

    private static LongBinaryTag fromNative(cn.nukkit.nbt.tag.LongTag other) {
        return LongBinaryTag.of(other.data);
    }

    private static StringBinaryTag fromNative(cn.nukkit.nbt.tag.StringTag other) {
        return StringBinaryTag.of(other.data);
    }

    private static IntBinaryTag fromNative(cn.nukkit.nbt.tag.IntTag other) {
        return IntBinaryTag.of(other.data);
    }

    private static ByteBinaryTag fromNative(cn.nukkit.nbt.tag.ByteTag other) {
        return ByteBinaryTag.of((byte) other.data);
    }

    private static ByteArrayBinaryTag fromNative(cn.nukkit.nbt.tag.ByteArrayTag other) {
        byte[] value = other.data;
        return ByteArrayBinaryTag.of(value);
    }

    private static FloatBinaryTag fromNative(cn.nukkit.nbt.tag.FloatTag other) {
        return FloatBinaryTag.of(other.data);
    }

    private static ShortBinaryTag fromNative(cn.nukkit.nbt.tag.ShortTag other) {
        return ShortBinaryTag.of((short) other.data);
    }

    private static DoubleBinaryTag fromNative(cn.nukkit.nbt.tag.DoubleTag other) {
        return DoubleBinaryTag.of(other.data);
    }

}

