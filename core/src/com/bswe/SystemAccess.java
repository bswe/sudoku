package com.bswe;

/**
 * Created by wcb on 10/1/2017.
 */

// platform interface to access platform specific resources and system calls
public interface SystemAccess {
    void WriteClipboard (String s);
    void RequestExternalAccess ();
}