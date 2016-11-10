package  gr.demokritos.iit.crawlers.twitter.utils;


/**
 * Created by npittaras on 4/11/2016.
 */

import java.io.*;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.io.RandomAccessFile;
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

    void read() throws IOException {

        Data = new ArrayList<>();

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
    public ArrayList<String> getData() throws IOException {

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

    public void append(ArrayList<String> data) throws IOException {

        FileWriter fw = new FileWriter(F,true);
        BufferedWriter bw = new BufferedWriter(fw);

        for(String datum : data)
        {
            bw.write(datum + "\n");
        }
        bw.close();
        fw.close();

    }
    public void deleteContents()
    {
        // Filewriter approach to deleting contents seems to actually delete the file,
        // effectively "releasing" the lock before unlock() is called.

        RandomAccessFile file  = null;
        try {
            file = new RandomAccessFile (F.getPath(),"rw");
            // truncate 20 last bytes of filename.ext
            file.setLength(0);

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

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
