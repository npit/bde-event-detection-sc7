package gr.demokritos.iit.crawlers.twitter.utils;

/**
 * Created by npittaras on 4/11/2016.
 */

import java.io.File;
import java.io.RandomAccessFile;
import java.nio.channels.FileLock;

/**
 *  Class to guarantee safe read access to a file that's being written to by
 *  an another process
 */
public class FileAccessor
{
    File F;
    boolean IsLocked;
    public FileAccessor(String path)
    {
        F = new File(path);
    }
    boolean exists(){ return F.exists(); }

    public void lock()
    {
        FileChannel channel = new RandomAccessFile(file, "rw").getChannel();
        FileLock lock =
    }

}
