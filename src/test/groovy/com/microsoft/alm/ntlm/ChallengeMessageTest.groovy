// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.ntlm

import groovy.transform.CompileStatic
import org.junit.Test

/**
 * A class to test {@link ChallengeMessage}
 */
@CompileStatic
public class ChallengeMessageTest {

    @Test
    public void flags_withExtendedSessionSecurity() {
        // truncated to just the pieces we can currently decode (flags)
        final input = "TlRMTVNTUAACAAAACgAKADgAAAAGgokC";

        final actual = new ChallengeMessage(input);

        assert (actual.flags & 0x00080000) == 0x00080000
    }

    @Test
    public void flags_random() {
        // truncated to just the pieces we can currently decode (flags)
        final input = "TlRMTVNTUAACAAAABwAHADgAAAAGgokC";

        final actual = new ChallengeMessage(input);

        assert (actual.flags & 0x00080000) == 0x00080000
    }
}
