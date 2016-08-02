// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.ntlm;

public class ChallengeMessage extends Message
{
    public ChallengeMessage(final String base64)
    {
        super(base64, 2);
        final byte[] targetNameFields = new byte[8];
        buf.get(targetNameFields);
        flags = buf.getInt();
    }
}
