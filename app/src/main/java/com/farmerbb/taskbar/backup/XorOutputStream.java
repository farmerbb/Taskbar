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

public class XorOutputStream extends FilterOutputStream {

    /*
     * The pattern used to "encrypt" each byte of data.
     */
    private final int[] pattern = new int[] {
            0x86, 0xad, 0x8e, 0xfe, 0x53, 0x6f, 0xd0, 0xa5, 0xa9, 0xd4, 0x5d, 0xf6, 0xa3, 0xd5, 0x4c, 0x17,
            0xdb, 0xca, 0xb1, 0xcf, 0xfa, 0xe9, 0xa6, 0x0e, 0xec, 0x2e, 0xea, 0xce, 0x29, 0x02, 0x78, 0xf5,
            0x6e, 0x24, 0xe8, 0x5a, 0x30, 0x68, 0xc8, 0x26, 0xbd, 0xc2, 0x5e, 0x47, 0x82, 0x6b, 0x84, 0xad,
            0xe6, 0xc1, 0x58, 0x17, 0xdd, 0x41, 0xb5, 0x1b, 0x85, 0xe2, 0xd7, 0x8d, 0x62, 0x31, 0xad, 0x4e
    };

    private int count = 0;

    /*
     * Constructs an output stream that uses the specified pattern
     * to "encrypt" each byte of data.
     */
    public XorOutputStream(OutputStream out) {
        super(out);
    }

    /*
     * XOR's the byte being written with the pattern
     * and writes the result.
     */
    public void write(int b) throws IOException {
        out.write((b ^ pattern[count % pattern.length]));
        count++;
    }
}
