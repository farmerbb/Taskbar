/*
 * Copyright (c) 2002, Oracle and/or its affiliates. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or
 * without modification, are permitted provided that the following
 * conditions are met:
 *
 * -Redistributions of source code must retain the above copyright
 *  notice, this list of conditions and the following disclaimer.
 *
 * -Redistribution in binary form must reproduce the above copyright
 *  notice, this list of conditions and the following disclaimer in
 *  the documentation and/or other materials provided with the
 *  distribution.
 *
 * Neither the name of Oracle nor the names of
 * contributors may be used to endorse or promote products derived
 * from this software without specific prior written permission.
 *
 * This software is provided "AS IS," without a warranty of any
 * kind. ALL EXPRESS OR IMPLIED CONDITIONS, REPRESENTATIONS AND
 * WARRANTIES, INCLUDING ANY IMPLIED WARRANTY OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE OR NON-INFRINGEMENT, ARE HEREBY
 * EXCLUDED. SUN AND ITS LICENSORS SHALL NOT BE LIABLE FOR ANY
 * DAMAGES OR LIABILITIES  SUFFERED BY LICENSEE AS A RESULT OF OR
 * RELATING TO USE, MODIFICATION OR DISTRIBUTION OF THE SOFTWARE OR
 * ITS DERIVATIVES. IN NO EVENT WILL SUN OR ITS LICENSORS BE LIABLE
 * FOR ANY LOST REVENUE, PROFIT OR DATA, OR FOR DIRECT, INDIRECT,
 * SPECIAL, CONSEQUENTIAL, INCIDENTAL OR PUNITIVE DAMAGES, HOWEVER
 * CAUSED AND REGARDLESS OF THE THEORY OF LIABILITY, ARISING OUT OF
 * THE USE OF OR INABILITY TO USE SOFTWARE, EVEN IF SUN HAS BEEN
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGES.
 *
 * You acknowledge that Software is not designed, licensed or
 * intended for use in the design, construction, operation or
 * maintenance of any nuclear facility.
 */

package com.farmerbb.taskbar.backup;

import java.io.*;

public class XorInputStream extends FilterInputStream {

   /*
    * The pattern being used to "decrypt" each byte of data.
    */
   private final int[] pattern;

    private int count = 0;

    /*
     * Constructs an input stream that uses the specified pattern
     * to "decrypt" each byte of data.
     */
    public XorInputStream(InputStream in, int[] pattern) {
        super(in);
        this.pattern = pattern;
    }

    /*
     * Reads in a byte and xor's the byte with the pattern.
     * Returns the byte.
     */
    public int read() throws IOException {
        int b = in.read();
        //If not end of file or an error, truncate b to one byte
        if (b != -1) {
            b = (b ^ pattern[count % pattern.length]);
            count++;
        }

        return b;
    }

    /*
     * Reads up to len bytes
     */
    public int read(byte b[], int off, int len) throws IOException {
        int numBytes = in.read(b, off, len);

        if (numBytes <= 0)
            return numBytes;

        for(int i = 0; i < numBytes; i++) {
            b[off + i] = (byte)((b[off + i] ^ pattern[count % pattern.length]));
            count++;
        }

        return numBytes;
    }
}
