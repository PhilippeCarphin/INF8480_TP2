package ca.polymtl.inf8480.tp1.shared;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.Files;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.Serializable;
import java.io.FileNotFoundException;

public class SyncedFile implements Serializable
{
	private String name;
	private byte[] content;
	private long checksum;

	public SyncedFile(String filePath)
	{
		String[] splitPath = filePath.split("/");
		this.name = splitPath[splitPath.length - 1];

		File file = new File(filePath);
		this.content = new byte[(int)file.length()];
		this.checksum = file.lastModified();

		try
		{
		FileInputStream fis = new FileInputStream(file);
		fis.read(this.content);
		fis.close();
		}
		catch (FileNotFoundException e)
		{
			e.printStackTrace();
			return;
		}
        catch (IOException e)
		{
			e.printStackTrace();
			return;
		}
	}

	public SyncedFile(String name, byte[] content)
	{
		this.name = name;
		this.content = content;
	}

	public byte[] getContent()
	{
		return this.content;
	}

	public String getName()
	{
		return this.name;
	}

	public long getChecksum()
	{
		return this.checksum;
	}

	public void writeOnDisk(String path)
	{
		try
		{
			Path file = Paths.get(path);
			Files.write(file, this.content);
		}
		catch (IOException e)
		{
			e.printStackTrace();
			return;
		}
	}
}
