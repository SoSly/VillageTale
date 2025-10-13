package org.sosly.villagetale.api.serialization;

import com.mojang.serialization.Codec;
import java.nio.ByteBuffer;
import java.util.UUID;

public class Codecs {
    public static final Codec<java.util.UUID> UUID_CODEC = Codec.BYTE_BUFFER.xmap(
        buffer -> {
            byte[] bytes = new byte[buffer.remaining()];
            buffer.get(bytes);
            ByteBuffer bb = ByteBuffer.wrap(bytes);
            return new UUID(bb.getLong(), bb.getLong());
        },
        uuid -> {
            ByteBuffer buffer = ByteBuffer.allocate(16);
            buffer.putLong(uuid.getMostSignificantBits());
            buffer.putLong(uuid.getLeastSignificantBits());
            buffer.flip();
            return buffer;
        }
    );
}
