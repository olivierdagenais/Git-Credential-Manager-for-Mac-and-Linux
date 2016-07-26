// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.authentication

import groovy.transform.CompileStatic
import org.junit.Test

/**
 * A class to test {@link VsTeam}.
 */
@CompileStatic
public class VsTeamTest {

    @Test public void looksLikeTfsGitPath_onPremises() {
        final input = "/tfs/DefaultCollection/Default/_git/CoolBeans";

        final actual = VsTeam.looksLikeTfsGitPath(input);

        assert actual
    }

    @Test public void looksLikeTfsGitPath_hosted() {
        final input = "/Default/_git/CoolBeans";

        final actual = VsTeam.looksLikeTfsGitPath(input);

        assert actual
    }

    @Test public void looksLikeTfsGitPath_gitHub() {
        final input = "/Microsoft/Git-Credential-Manager-for-Mac-and-Linux.git";

        final actual = VsTeam.looksLikeTfsGitPath(input);

        assert !actual
    }


    @Test public void isTeamFoundationServer_noUnderscoreGitInPath() throws Exception
    {
        final input = URI.create("https://github.com/Microsoft/Git-Credential-Manager-for-Mac-and-Linux.git");

        final boolean actual = VsTeam.isTeamFoundationServer(input);

        assert !actual
    }

}
