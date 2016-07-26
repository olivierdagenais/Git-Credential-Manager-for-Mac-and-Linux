// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.authentication;

import com.microsoft.alm.helpers.Action;
import com.microsoft.alm.helpers.HttpClient;
import com.microsoft.alm.helpers.IOHelper;
import com.microsoft.alm.secret.Credential;

import java.io.Closeable;
import java.io.IOException;
import java.net.Authenticator;
import java.net.HttpURLConnection;
import java.net.PasswordAuthentication;
import java.net.URI;

public class VsTeam
{
    private static final String X_TFS_PROCESS_ID = "X-TFS-ProcessId";
    private static final String X_VSS_RESOURCE_TENANT = "X-VSS-ResourceTenant";

    public static boolean looksLikeTfsGitPath(final String uriPath)
    {
        final String[] parts = uriPath.split("/");
        boolean result = false;
        for (int i = 0; i < parts.length; i++)
        {
            final String part = parts[i];
            if (part.equalsIgnoreCase("_git"))
            {
                if (i == parts.length - 2)
                {
                    result = true;
                    break;
                }
            }
        }
        return result;
    }

    public static boolean isTeamFoundationServer(final URI repoUri) throws IOException
    {
        final String repoUriPath = repoUri.getPath();
        if (!looksLikeTfsGitPath(repoUriPath))
        {
            return false;
        }

        final HttpClient client = new HttpClient(Global.getUserAgent());
        final HttpURLConnection response = client.head(repoUri);
        final String tfsProcessId = response.getHeaderField(X_TFS_PROCESS_ID);
        if (tfsProcessId != null)
        {
            // it could still be Team Services; if so, it will have the X-VSS-ResourceTenant header
            final String resourceTenant = response.getHeaderField(X_VSS_RESOURCE_TENANT);
            return resourceTenant == null;
        }
        return false;
    }

    public static class PasswordAuthenticator extends Authenticator implements Closeable
    {
        private final String userName;
        private final char[] password;

        public PasswordAuthenticator(final String userName, final char[] password)
        {
            this.userName = userName;
            this.password = password;
        }

        @Override
        protected PasswordAuthentication getPasswordAuthentication()
        {
            return new PasswordAuthentication(this.userName, this.password);
        }

        @Override
        public void close() throws IOException
        {
            for (int i = 0; i < password.length; i++)
            {
                password[i] = '\0';
            }
        }
    }

    public static boolean areCredentialsValid(final URI repoUri, final Credential credential) throws IOException
    {
        final PasswordAuthenticator authenticator = new PasswordAuthenticator(credential.Username, credential.Password.toCharArray());
        Authenticator.setDefault(authenticator);
        try
        {
            final HttpClient client = new HttpClient(Global.getUserAgent());
            final HttpURLConnection response = client.get(repoUri);
            final int responseCode = response.getResponseCode();
            return responseCode == HttpURLConnection.HTTP_OK;
        }
        finally
        {
            IOHelper.closeQuietly(authenticator);
            Authenticator.setDefault(null);
        }
    }
}
