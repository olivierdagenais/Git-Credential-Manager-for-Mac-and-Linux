// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.authentication;

import com.microsoft.alm.gitcredentialmanager.TestProcess;
import com.microsoft.alm.helpers.Func;
import com.microsoft.alm.helpers.IOHelper;
import com.microsoft.alm.helpers.StringHelper;
import com.microsoft.alm.oauth2.useragent.subprocess.TestableProcess;
import com.microsoft.alm.oauth2.useragent.subprocess.TestableProcessFactory;
import org.junit.Assert;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.concurrent.atomic.AtomicReference;

import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class WhereTest
{
    @Test public void app_ping() throws Exception
    {
        final AtomicReference<String> path = new AtomicReference<String>();

        final boolean success = Where.app("ping", path);
        final String actual = path.get();

        Assert.assertEquals("Doesn't ping exist on all platforms?", true, success);
        Assert.assertNotNull(actual);
    }
    
    @Test public void app_simulateWindowsSuccess()
    {
        final AtomicReference<String> path = new AtomicReference<String>();
        final String envpath = "c:/Windows;c:/Windows/System32;c:/Java";
        @SuppressWarnings("unchecked")
        final Func<String, Boolean> existenceChecker = (Func<String, Boolean>) mock(Func.class);
        final String pathValue = "c:/Java/java.exe";
        when(existenceChecker.call(pathValue)).thenReturn(true);

        final boolean result = Where.app("java", path, ".exe", envpath, ";", existenceChecker);

        Assert.assertEquals(true, result);
        Assert.assertEquals(pathValue, path.get());
        verify(existenceChecker, times(3)).call(anyString());
    }

    @Test public void app_simulateWindowsFailure()
    {
        final AtomicReference<String> path = new AtomicReference<String>();
        final String envpath = "c:/Windows;c:/Windows/System32;c:/Java";
        @SuppressWarnings("unchecked")
        final Func<String, Boolean> existenceChecker = (Func<String, Boolean>) mock(Func.class);
        when(existenceChecker.call("c:/Java/java.exe")).thenReturn(false);

        final boolean result = Where.app("java", path, ".exe", envpath, ";", existenceChecker);

        Assert.assertEquals(false, result);
        verify(existenceChecker, times(3)).call(anyString());
    }

    @Test public void app_simulateNixSuccess()
    {
        final AtomicReference<String> path = new AtomicReference<String>();
        final String envpath = "/usr/bin:/usr/sbin:/opt/java";
        @SuppressWarnings("unchecked")
        final Func<String, Boolean> existenceChecker = (Func<String, Boolean>) mock(Func.class);
        final String pathValue = "/opt/java/java";
        when(existenceChecker.call(pathValue)).thenReturn(true);

        final boolean result = Where.app("java", path, StringHelper.Empty, envpath, ":", existenceChecker);

        Assert.assertEquals(true, result);
        Assert.assertEquals(pathValue, path.get());
        verify(existenceChecker, times(3)).call(anyString());
    }
    
    @Test public void app_simulateNixFailure()
    {
        final AtomicReference<String> path = new AtomicReference<String>();
        final String envpath = "/usr/bin:/usr/sbin:/opt/java";
        @SuppressWarnings("unchecked")
        final Func<String, Boolean> existenceChecker = (Func<String, Boolean>) mock(Func.class);
        when(existenceChecker.call("/opt/java")).thenReturn(false);

        final boolean result = Where.app("java", path, StringHelper.Empty, envpath, ":", existenceChecker);

        Assert.assertEquals(false, result);
        verify(existenceChecker, times(3)).call(anyString());
    }

    @Test public void findCurlVersionInStrings_Fedora() throws Exception
    {
        final Class<? extends WhereTest> me = this.getClass();
        final BufferedReader br = new BufferedReader(new InputStreamReader(me.getResourceAsStream("strings_libcurl.txt")));
        try
        {
            final String actual = Where.findCurlVersionInStrings(br);
            Assert.assertEquals("7.40.0", actual);
        }
        finally
        {
            br.close();
        }
    }

    @Test public void findLibCurlInLdd_Fedora() throws Exception
    {
        final Class<? extends WhereTest> me = this.getClass();
        final BufferedReader br = new BufferedReader(new InputStreamReader(me.getResourceAsStream("ldd_git-http-fetch.txt")));
        try
        {
            final String actual = Where.findLibCurlInLdd(br);
            Assert.assertEquals("/lib64/libcurl.so.4", actual);
        }
        finally
        {
            br.close();
        }
    }

    @Test public void libcurl_fedora() throws Exception
    {
        final Class<? extends WhereTest> me = this.getClass();
        final InputStream inputStream = me.getResourceAsStream("ldd_git-http-fetch.txt");
        final String input = IOHelper.readToString(inputStream);
        inputStream.close();
        final String gitHttpFetchPath = "/usr/libexec/git-core/git-http-fetch";
        final TestableProcess process = new TestProcess(input + "\n");
        final TestableProcessFactory processFactory = new TestableProcessFactory()
        {
            @Override public TestableProcess create(final String... strings) throws IOException
            {
                Assert.assertEquals("ldd", strings[0]);
                return process;
            }
        };
        final File gitHttpFetch = new File(gitHttpFetchPath);

        final File actual = Where.libcurl(processFactory, gitHttpFetch);
        
        Assert.assertEquals(new File("/lib64/libcurl.so.4"), actual);
    }
}
