/* Copyright 2016 NCSR Demokritos
*
*  Licensed under the Apache License, Version 2.0 (the "License");
*  you may not use this file except in compliance with the License.
*  You may obtain a copy of the License at
*
*    http://www.apache.org/licenses/LICENSE-2.0
*
*  Unless required by applicable law or agreed to in writing, software
*  distributed under the License is distributed on an "AS IS" BASIS,
*  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
*  See the License for the specific language governing permissions and
*  limitations under the License.
*/
package gr.demokritos.iit.crawlers;

import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackInputStream;

// Taken from http://forums.sun.com/thread.jspa?threadID=5310966&messageID=10324405#10324405

public class BOMStripperInputStream extends PushbackInputStream
{
    public static final int[][] BOMS =
    {
        {
            0x00, 0x00, 0xFE, 0xFF
        },
        {
            0xFF, 0xFE, 0x00, 0x00
        },
        {
            0x2B, 0x2F, 0x76, 0x38
        },
        {
            0x2B, 0x2F, 0x76, 0x39
        },
        {
            0x2B, 0x2F, 0x76, 0x2B
        },
        {
            0x2B, 0x2F, 0x76, 0x2F
        },
        {
            0xDD, 0x73, 0x66, 0x73
        },
        {
            0xEF, 0xBB, 0xBF
        },
        {
            0x0E, 0xFE, 0xFF
        },
        {
            0xFB, 0xEE, 0x28
        },
        {
            0xFE, 0xFF
        },
        {
            0xFF, 0xFE
        }
    };

    static private int testForBOM(int[] bom, int[] bytes)
    {
        for (int index = 0; index < bom.length; index++)
        {
            if (bom[index] != bytes[index])
                return 0;
        }
        return bom.length;
    }

    public BOMStripperInputStream(InputStream is) throws IOException
    {
        super(is, 4);

        final int[] bytes =
        {
            read(), read(), read(), read()
        };
        int count = 0;
        for (int[] bom : BOMS)
        {
            count = testForBOM(bom, bytes);
            if (count != 0)
                break;
        }
        for (int index = bytes.length - 1; index >= count; index--)
        {
            if (bytes[index] != -1)
                unread(bytes[index]);
        }
    }
}

