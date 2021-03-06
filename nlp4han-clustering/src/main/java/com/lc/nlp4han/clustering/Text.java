package com.lc.nlp4han.clustering;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;


public class Text
{
	private static int defaultName = 1;
	private String name;
	private String content;
	private Sample sample;
	
	public Text()
	{
		
	}
	
	public Text(String name, String content)
	{
		this.name = name;
		this.content = content;
	}

	public Text(File f)
	{
		
	}

	public Text(String content)
	{
		this.content = content;
		this.name = defaultName + "";
		defaultName++;
	}

	public static List<Text> getTexts(String folderPath, boolean useDefaultName)
	{
		List<Text> result = new ArrayList<Text>();
		List<File> files = travaringFilesForLevelTraversal(folderPath);
		
		for (File file : files)
		{
			Text t = processingFile(file, useDefaultName);
			if (t != null)
				result.add(t);
		}
		
		return result;
	}
	
	public void generateSample(SampleGenerator sg, FeatureGenerator fg)
	{
		this.sample = sg.getSample(this, fg);
	}

	public Sample getSample()
	{
		return this.sample;
	}

	public void setSample(Sample s)
	{
		this.sample = s;
		
	}

	public String getName()
	{
		return name;
	}

	public String getContent()
	{
		return content;
	}
	
	/**
	 * 层次遍历pos文件夹下所有文件
	 * @param pos 待遍历的文件夹
	 * @return 文件夹pos下的所有文件列表
	 */
	private static List<File> travaringFilesForLevelTraversal(String pos)
	{
		List<File> files = new LinkedList<File>();
		List<File> result = new LinkedList<File>();
		File tmpFile = new File(pos);
		if (pos == null)
		{
			throw new RuntimeException("地址为空");
		}
		if (!tmpFile.exists())
		{
			throw new RuntimeException("文件地址不存在");
		}
		
		files.add(tmpFile);
		while (!files.isEmpty())
		{
			File[] subFiles;
			tmpFile = files.remove(0);
			if (tmpFile.isFile())
			{
				result.add(tmpFile);
			}
			else if (tmpFile.isDirectory())
			{
				subFiles = tmpFile.listFiles();
				for (int i=0 ; i<subFiles.length ; i++)
				{
					files.add(subFiles[i]);
				}
			}
		}
		
		return result;
		
	}

	
	/**
	 * 处理文档
	 * @param f 待处理的文档
	 * @param useDefaultName 是否使用默认名字
	 * @param texts 存储生成的文本层文本
	 */
	private static Text processingFile(File f, boolean useDefaultName)
	{
		
		StringBuffer content = new StringBuffer();
		String tmp;
		Reader read;
		BufferedReader bufr;
		try
		{
			read = new FileReader(f);
			bufr = new BufferedReader(read);
		
			while ((tmp=bufr.readLine()) != null)
			{
				content.append(tmp);
			}
			bufr.close();
		} catch (IOException e)
		{
			e.printStackTrace();
		}
	
		if (content.length() > 0)
		{
			Text t = null;
			if (useDefaultName || f.getName() == null || f.getName() == "")
			{
				t = new Text(content.toString());
			}
			else
			{
				t = new Text(f.getName(), content.toString());
			}
				
			return t;
		}
		else
			return null;
		
	}

}
