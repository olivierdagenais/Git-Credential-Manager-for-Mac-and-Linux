// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.ntlm;

import javax.xml.bind.DatatypeConverter;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public abstract class Message
{
    private static final Map<Integer, String> FLAGS;

    static {
        final Map<Integer, String> map = new LinkedHashMap<Integer, String>();
        map.put(0x00000001, "Negotiate Unicode");
        map.put(0x00000002, "Negotiate OEM");
        map.put(0x00000004, "Request Target");
        map.put(0x00000008, "unused");
        map.put(0x00000010, "Negotiate Sign");
        map.put(0x00000020, "Negotiate Seal");
        map.put(0x00000040, "Negotiate Datagram");
        map.put(0x00000080, "Negotiate LM Key");
        map.put(0x00000100, "unused");
        map.put(0x00000200, "Negotiate NTLM v1");
        map.put(0x00000400, "unused");
        map.put(0x00000800, "Anonymous");
        map.put(0x00001000, "Negotiate OEM Domain Supplied");
        map.put(0x00002000, "Negotiate OEM Workstation Supplied");
        map.put(0x00004000, "unused");
        map.put(0x00008000, "Negotiate Always Sign");
        map.put(0x00010000, "Target Type Domain");
        map.put(0x00020000, "Target Type Server");
        map.put(0x00040000, "unused");
        map.put(0x00080000, "Negotiate Extended Session Security");
        map.put(0x00100000, "Negotiate Identify");
        map.put(0x00200000, "unused");
        map.put(0x00400000, "Request Non-NT Session Key");
        map.put(0x00800000, "Negotiate Target Info");
        map.put(0x01000000, "unused");
        map.put(0x02000000, "Negotiate Version");
        map.put(0x04000000, "unused");
        map.put(0x08000000, "unused");
        map.put(0x10000000, "unused");
        map.put(0x20000000, "Negotiate 128");
        map.put(0x40000000, "Negotiate Key Exchange");
        map.put(0x80000000, "Negotiate 56");
        FLAGS = Collections.unmodifiableMap(map);
    }

    protected final ByteBuffer buf;
    protected final int messageType;
    protected int flags;

    public int getFlags()
    {
        return flags;
    }

    protected Message(final String base64, final int expectedMessageType)
    {
        final byte[] bytes = DatatypeConverter.parseBase64Binary(base64);
        buf = ByteBuffer.wrap(bytes);
        buf.order(ByteOrder.LITTLE_ENDIAN);
        final byte[] signature = new byte[8];
        buf.get(signature);
        // TODO: check signature is NTLMSSP\0
        messageType = buf.getInt();
        if (messageType != expectedMessageType)
        {
            throw new IllegalArgumentException("MessageType did not match!");
        }
    }

    @Override
    public String toString()
    {
        final StringBuilder sb = new StringBuilder();
        final String eol = "\n";
        sb.append("Message Type: ").append(messageType).append(eol);
        sb.append("Flags :").append(eol);
        for (final Map.Entry<Integer, String> entry : FLAGS.entrySet())
        {
            final Integer flag = entry.getKey();
            if ((flags & flag) == flag)
            {
                final String description = entry.getValue();
                sb.append("    ").append(description).append(eol);
            }
        }
        return sb.toString();
    }
}
