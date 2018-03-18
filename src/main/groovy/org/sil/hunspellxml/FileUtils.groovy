package org.sil.hunspellxml

import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.regex.Pattern
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class FileUtils
{
	static copy(fromFile, toFile)
	{
		if(fromFile && !(fromFile instanceof File))
		{
			fromFile = new File(fromFile)
		}
		if(toFile && !(toFile instanceof File))
		{
			toFile = new File(toFile)
		}
		
		def fos = new FileOutputStream(toFile)
		def fis = new FileInputStream(fromFile)
		fos << fis
		
		fis.close()
		fos.close()
	}
	
	static zip(topDir, zipFile)
	{
		zip(topDir, zipFile, null)
	}
	
	static zip(topDir, zipFile, ignorePattern)
	{
		if(ignorePattern && !(ignorePattern instanceof Pattern))
		{
			ignorePattern = Pattern.compile(ignorePattern)
		}
	
		if(topDir && !(topDir instanceof File))
		{
			topDir = new File(topDir)
		}
		else
		{
			topDir = new File('')
		}
		
		ZipOutputStream zipOutput = new ZipOutputStream(new FileOutputStream(zipFile));
	
		int topDirLength = topDir.absolutePath.length() + 1  //+1 is to get rid of the leading slash
	
		topDir.eachFileRecurse{ file ->
		  def relative = file.absolutePath.substring(topDirLength).replace('\\', '/') 
		  if ( file.isDirectory() && !relative.endsWith('/')){
		    relative += "/"
		  }
		  if( ignorePattern && ignorePattern.matcher(relative).find() ){
		    return
		  }
	
		  ZipEntry entry = new ZipEntry(relative)
		  entry.time = file.lastModified()
		  zipOutput.putNextEntry(entry)
		  if( file.isFile() ){
			def fis = new FileInputStream(file)
		    zipOutput << fis
		    fis.close()
		  }
		}
		zipOutput.close()
	}
	
	
	static public boolean deleteDirectory(path)
	{
		if(path && !(path instanceof File))
		{
			path = new File(path)
		}
		
		if( path.exists() )
		{
			File[] files = path.listFiles();
			for(int i=0; i<files.length; i++)
			{
				if(files[i].isDirectory())
				{
					deleteDirectory(files[i]);
				}
				else
				{
					files[i].delete();
				}
			}
		}
		return( path.delete() );
	}
}

