// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.authentication

import com.github.tomakehurst.wiremock.junit.WireMockRule
import groovy.transform.CompileStatic
import org.junit.Rule
import org.junit.Test

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse
import static com.github.tomakehurst.wiremock.client.WireMock.head
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo

/**
 * A class to test {@link VsTeam}.
 */
@CompileStatic
public class VsTeamTest {

    private static final String PROTOCOL = "http";

    private final String host;

    @Rule public WireMockRule wireMockRule = new WireMockRule(0);

    public VsTeamTest() {
        final def localHostAddress = InetAddress.localHost;
        host = localHostAddress.hostName;
    }

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


    @Test public void isTeamFoundationServer_onPremises() throws Exception
    {
        final port = wireMockRule.port();
        final path = "/tfs/DefaultCollection/Default/_git/CoolBeans"
        final uri = new URI(PROTOCOL, null, host, port, path, null, null)
        stubFor(head(urlEqualTo(path))
            .willReturn(aResponse()
                .withStatus(401)
                .withHeader("X-TFS-ProcessId", "4951a08b-74f1-43c5-8936-02fe990819a1")
            )
        );

        final boolean actual = VsTeam.isTeamFoundationServer(uri);

        assert actual
    }

    @Test public void isTeamFoundationServer_hosted() throws Exception
    {
        final port = wireMockRule.port();
        final path = "/Default/_git/CoolBeans"
        final uri = new URI(PROTOCOL, null, host, port, path, null, null)
        stubFor(head(urlEqualTo(path))
            .willReturn(aResponse()
                .withStatus(302)
                .withHeader("X-TFS-ProcessId", "edb078d8-81ca-4577-8bd7-d15b3afba154")
                .withHeader("X-VSS-ResourceTenant", "141f93f0-73ba-48c2-ba25-a646fb503e8b")
            )
        );

        final boolean actual = VsTeam.isTeamFoundationServer(uri);

        assert !actual
    }
}
