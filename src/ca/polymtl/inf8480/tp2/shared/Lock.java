package ca.polymtl.inf8480.tp1.shared;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.Files;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.Serializable;
import java.io.FileNotFoundException;

import ca.polymtl.inf8480.tp1.shared.SyncedFile;

public class Lock implements Serializable
{
    private int lockID;
    private SyncedFile file = null;

    public Lock(int lockID)
    {
        this.lockID = lockID;
    }

    public SyncedFile getSyncedFile()
    {
        return file;
    }

    public void setSyncedFile(String filePath)
    {
        this.file = new SyncedFile(filePath);
    }

    public int getLockID()
    {
        return this.lockID;
    }
}