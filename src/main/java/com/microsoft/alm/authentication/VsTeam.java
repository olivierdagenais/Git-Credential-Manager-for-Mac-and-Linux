// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.authentication;

import com.microsoft.alm.helpers.HttpClient;
import com.microsoft.alm.helpers.IOHelper;
import com.microsoft.alm.helpers.StringHelper;
import com.microsoft.alm.secret.Credential;

import java.io.Closeable;
import java.io.IOException;
import java.net.Authenticator;
import java.net.HttpURLConnection;
import java.net.PasswordAuthentication;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class VsTeam
{
    private static final String NEGOTIATE = "Negotiate";
    private static final String NTLM = "NTLM";
    private static final String WWW_AUTHENTICATE = "WWW-Authenticate";
    private static final String X_TFS_PROCESS_ID = "X-TFS-ProcessId";
    private static final String X_VSS_RESOURCE_TENANT = "X-VSS-ResourceTenant";

    private final URI repoUri;
    private final HttpClient client;
    private final Map<String, List<String>> headerFields;

    public VsTeam(final URI repoUri)
    {
        this.repoUri = repoUri;
        this.client = new HttpClient(Global.getUserAgent());
        try
        {
            final HttpURLConnection response = client.head(repoUri);
            this.headerFields = response.getHeaderFields();
        }
        catch (final IOException e)
        {
            throw new Error(e);
        }
    }

    public boolean isTeamFoundationServer()
    {
        if (hasValue(X_TFS_PROCESS_ID))
        {
            // it could still be Team Services; if so, it will have the X-VSS-ResourceTenant header
            return !hasValue(X_VSS_RESOURCE_TENANT);
        }
        return false;
    }

    public boolean looksLikeTfsGitPath()
    {
        final String repoUriPath = repoUri.getPath();
        return looksLikeTfsGitPath(repoUriPath);
    }

    public boolean offersNtlm()
    {
        return hasValue(WWW_AUTHENTICATE, NTLM);
    }

    public boolean offersNegotiate()
    {
        return hasValue(WWW_AUTHENTICATE, NEGOTIATE);
    }

    boolean hasValue(final String headerName, final String targetValue)
    {
        final List<String> headerValues = headerFields.get(headerName);
        if (headerValues != null && headerValues.size() > 0)
        {
            return headerValues.contains(targetValue);
        }
        return false;
    }

    boolean hasValue(final String headerName)
    {
        final List<String> headerValues = headerFields.get(headerName);
        return hasValue(headerValues);
    }

    static boolean hasValue(final List<String> headerValues)
    {
        return headerValues != null && headerValues.size() > 0;
    }

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

    public static boolean isTeamFoundationServer(final URI repoUri)
    {
        final VsTeam vsTeam = new VsTeam(repoUri);
        return vsTeam.isTeamFoundationServer();
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

    public static URI createHomeUri(final URI uri)
    {
        final String uriPath = uri.getPath();
        if (uriPath == null || uriPath.length() == 0 || uriPath.equals("/"))
        {
            return null;
        }
        final String[] parts = uriPath.split("/");
        final ArrayList<String> pathSegments = new ArrayList<String>(Arrays.asList(parts));
        int i = 0;
        for (; i < parts.length; i++)
        {
            final String part = parts[i];
            if (part.equalsIgnoreCase("_git"))
            {
                pathSegments.set(i, "_home");
                while (pathSegments.size() > i + 1)
                {
                    pathSegments.remove(pathSegments.size() - 1);
                }
                pathSegments.add("About");
                break;
            }
            if (part.equalsIgnoreCase("_home"))
            {
                if (i < 2)
                {
                    return null;
                }
                pathSegments.remove(i - 1);
                break;
            }
        }
        final String[] newParts = pathSegments.toArray(new String[pathSegments.size()]);
        final String newPath = StringHelper.join("/", newParts);
        try
        {
            return new URI(uri.getScheme(), uri.getUserInfo(), uri.getHost(), uri.getPort(), newPath, null, null);
        }
        catch (URISyntaxException e)
        {
            throw new Error(e);
        }
    }

    public HttpURLConnection authenticatedHttp(final Credential credential)
    {
        return authenticatedHttp(credential, repoUri);
    }

    public HttpURLConnection authenticatedHttp(final Credential credential, final URI uri)
    {
        final PasswordAuthenticator authenticator = new PasswordAuthenticator(credential.Username, credential.Password.toCharArray());
        Authenticator.setDefault(authenticator);
        try
        {
            final HttpURLConnection response = client.get(uri);
            return response;
        }
        catch (final IOException e)
        {
            // TODO: maybe we should just return false?
            throw new Error(e);
        }
        finally
        {
            IOHelper.closeQuietly(authenticator);
            Authenticator.setDefault(null);
        }
    }

    public boolean areCredentialsValid(final Credential credential)
    {
        final HttpURLConnection response = authenticatedHttp(credential);
        final int responseCode;
        try
        {
            responseCode = response.getResponseCode();
        }
        catch (final IOException e)
        {
            throw new Error(e);
        }
        return responseCode == HttpURLConnection.HTTP_OK;
    }

    public static boolean areCredentialsValid(final URI repoUri, final Credential credential)
    {
        final VsTeam vsTeam = new VsTeam(repoUri);
        return vsTeam.areCredentialsValid(credential);
    }
}
