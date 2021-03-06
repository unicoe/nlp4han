package com.lc.nlp4han.constituent.pcfg;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

import com.lc.nlp4han.constituent.BracketExpUtil;
import com.lc.nlp4han.constituent.PlainTextByTreeStream;
import com.lc.nlp4han.constituent.TreeNode;
import com.lc.nlp4han.ml.util.FileInputStreamFactory;

/**
 * 文法抽取工具类
 * 
 * 从树库中抽取CFG或PCFG文法
 * 
 */
public class GrammarExtractor
{
	public static CFG getCFG(String fileName, String enCoding) throws IOException
	{
		return extractGrammar(fileName, enCoding, "CFG");
	}

	public static PCFG getPCFG(String fileName, String enCoding) throws IOException
	{

		return (PCFG) extractGrammar(fileName, enCoding, "PCFG");
	}

	private static CFG extractGrammar(String fileName, String enCoding, String type) throws IOException
	{
		// 括号表达式树拼接成括号表达式String数组
		PlainTextByTreeStream ptbt = new PlainTextByTreeStream(new FileInputStreamFactory(new File(fileName)),
				enCoding);
		String bracketStr = ptbt.read();
		ArrayList<String> bracketStrList = new ArrayList<String>();
		while (bracketStr!=null)
		{
			bracketStrList.add(bracketStr);
			bracketStr = ptbt.read();
		}
		ptbt.close();

		// 括号表达式生成文法
		CFG grammar = brackets2Grammar(bracketStrList, type);

		return grammar;

	}

	// 由括号表达式的list得到对应的文法集合
	private static CFG brackets2Grammar(ArrayList<String> bracketStrList, String type) throws IOException
	{
		HashMap<PRule, Integer> ruleCounter = null;
		CFG grammar = null;
		if (type.contains("P"))
		{
			grammar = new PCFG();
			ruleCounter = new HashMap<PRule, Integer>();
		}
		else
		{
			grammar = new CFG();
		}

		for (String bracketStr : bracketStrList)
		{
			TreeNode rootNode1 = BracketExpUtil.generateTree(bracketStr);
			traverse(rootNode1, grammar, ruleCounter);
		}

		if (type.contains("P"))
		{
			ComputeProOfRule(grammar, ruleCounter);
		}

		return grammar;
	}

	/**
	 * 遍历树得到基本文法
	 */
	private static void traverse(TreeNode node, CFG grammar, HashMap<PRule, Integer> ruleCounter)
	{
		if (grammar.getStartSymbol() == null)
		{// 起始符提取
			grammar.setStartSymbol(node.getNodeName());
		}

		if (node.getChildren().size() == 0)
		{
			grammar.addTerminal(node.getNodeName());// 终结符提取
			return;
		}

		grammar.addNonTerminal(node.getNodeName());// 非终结符提取

		if (node.getChildren() != null && node.getChildren().size() > 0)
		{
			RewriteRule rule = new RewriteRule(node.getNodeName(), node.getChildren());
			if (grammar instanceof PCFG)
			{
				rule = new PRule(rule, 0);
				addRuleCount((PRule) rule, ruleCounter);
			}

			grammar.add(rule);// 添加规则

			for (TreeNode node1 : node.getChildren())
			{// 深度优先遍历
				traverse(node1, grammar, ruleCounter);
			}
		}
	}

	private static void addRuleCount(PRule rule, HashMap<PRule, Integer> ruleCounter)
	{
		if (ruleCounter.containsKey(rule))
		{
			ruleCounter.put(rule, ruleCounter.get(rule) + 1);
		}
		else
		{
			ruleCounter.put(rule, 1);
		}
	}

	private static void ComputeProOfRule(CFG grammar, HashMap<PRule, Integer> ruleCounter)
	{
		for (String nonTer : grammar.getNonTerminalSet())
		{
			Set<RewriteRule> set = grammar.getRuleBylhs(nonTer);
			int allNum = 0;
			for (RewriteRule rule : set)
			{
				PRule pr = (PRule) rule;
				allNum += ruleCounter.get(pr);
			}

			for (RewriteRule rule : set)
			{
				PRule pr = (PRule) rule;
				pr.setProb(1.0 * ruleCounter.get(rule) / allNum);
			}
		}
	}

	/**
	 * 由括号表达式列表直接得到PCFG
	 */
	public static PCFG getPCFG(ArrayList<String> bracketStrList) throws IOException
	{
		CFG grammar = brackets2Grammar(bracketStrList, "PCFG");

		return (PCFG) grammar;
	}
}
