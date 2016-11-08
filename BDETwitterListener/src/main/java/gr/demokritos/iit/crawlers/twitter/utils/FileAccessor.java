package gr.demokritos.iit.crawlers.twitter.utils;

/**
 * Created by npittaras on 4/11/2016.
 */

import java.io.*;
import java.lang.reflect.Array;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.util.ArrayList;

/**
 *  Class to guarantee safe read access to a file that's being written to by
 *  an another process
 */
public class FileAccessor
{

    static final long SLEEPTIME = 5000l;
    File F;
    FileChannel Channel;
    FileLock Lock;
    boolean IsLocked;
    ArrayList<String> Data;

    public FileAccessor(String path)
    {
        F = new File(path);
        System.out.println("Initiated file accessor on  [" + F.getAbsolutePath() + "]");

    }
    boolean exists(){ return F.exists(); }

    void read()
    {

        Data = new ArrayList<>();
        try{
            FileInputStream in = new FileInputStream(F);

                BufferedReader R = new BufferedReader(new InputStreamReader(in));
                String line;
                while( (line = R.readLine()) != null)
                {
                    line = line.trim();
                    if(line.isEmpty()) continue;
                    Data.add(line);
                }
            }
        catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
    public ArrayList<String> getData()
    {

        read();
//        try {
//            read();
//        } catch (IOException e) {
//            e.printStackTrace();
//            try {
//                Thread.sleep(SLEEPTIME);
//            } catch (InterruptedException e1) {
//                e1.printStackTrace();
//            }
//            return getData();
//        }

        return Data;
    }
    public boolean lock()
    {

        try {
            Channel = new RandomAccessFile(F, "rw").getChannel();
            System.out.print("Attempting to lock...");
            Lock = Channel.lock();
            System.out.println("done!");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return false;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
            return true;
    }

    public void unlock()
    {

         try {
             if(Lock != null)
                 Lock.release();
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("Released lock on file [ " + F.getAbsolutePath() + "].");
    }

    public void close()
    {
        try {
            Channel.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
