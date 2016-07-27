// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.ntlm;

/**
 * LAN Manager authentication level
 * See https://technet.microsoft.com/en-us/library/jj852207(v=ws.10).aspx
 */
public enum AuthenticationLevel
{
    NOT_APPLICABLE,
    LM,
    NTLM,
    NTLMv2,
    ;
}
