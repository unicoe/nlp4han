package com.lc.nlp4han.constituent.pcfg;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 文法转换器
 *
 */
public class GrammarConvertor
{

	public static CFG CFG2CNF(CFG cfg)
	{
		return convertGrammar("CNF", cfg, new CFG());
	}

	/**
	 * 包含单元规则的CNF文法
	 * 
	 * 允许A->B类型的规则
	 * 
	 * @param pcfg
	 * @return
	 */
	public static PCFG PCFG2LoosePCNF(PCFG pcfg)
	{
		return (PCFG) convertGrammar("P2NF", pcfg, new PCFG());
	}

	/**
	 * 这里的转换为PCNF在消除单元规则时,消除到POS层次即停止
	 * 
	 * @param pcfg
	 * @return
	 */
	public static PCFG PCFG2PCNF(PCFG pcfg)
	{
		return (PCFG)convertGrammar("PCNF", pcfg, new PCFG());
	}

	/**
	 * 转换的通用类
	 * 
	 * @param type
	 * @param cfg
	 */
	private static CFG convertGrammar(String type, CFG cfg, CFG cnf)
	{
		toLooseCNF(cfg, type, cnf);

		if (!type.contains("2"))
		{// P2NF不需要消除单元规则
			HashSet<String> posSet = getPOSSet(cnf);
			
			removeUnitProduction(type, posSet, cnf);
		}
		
		return cnf;
	}

	/**
	 * 将规则转换为2nf形式（即不消除单元规则的乔姆斯基范式）
	 * 
	 * @param cfg
	 */
	private static void toLooseCNF(CFG cfg, String type, CFG cnf)
	{
		cnf.setNonTerminalSet(cfg.getNonTerminalSet());
		cnf.setTerminalSet(cfg.getTerminalSet());
		cnf.setStartSymbol(cfg.getStartSymbol());

		// 前期处理，遍历pcfg将规则加入pcnf
		reduceAndNormRight(cfg, type, cnf);
	}

	/**
	 * 前期处理，遍历的将规则加入pcnf 将字符串个数多于两个的递归的减为两个 将终结符和非终结符混合转换为两个非终结符 直接添加右侧只有一个字符串的规则
	 */
	private static void reduceAndNormRight(CFG cfg, String type, CFG cnf)
	{
		for (RewriteRule rule : cfg.getRuleSet())
		{
			if (rule.getRhs().size() >= 3)
			{
				// 如果右侧中有终结符，则转换为伪非终结符
				if (!cnf.getNonTerminalSet().containsAll(rule.getRhs()))
				{
					ConvertToNonTerRHS(rule, type, cnf);
				}

				reduceRHSNum(rule, type, cnf);
			}

			// 先检测右侧有两个字符串的规则是否为终结符和非终结符混合，若混合则先将终结符转换为非终结符
			if (rule.getRhs().size() == 2)
			{
				// 如果右侧中有终结符，则转换为伪非终结符
				if (!cnf.getNonTerminalSet().containsAll(rule.getRhs()))
				{
					ConvertToNonTerRHS(rule, type, cnf);
				}

				cnf.add(rule);
			}

			// 先添加进cnf随后处理
			if (rule.getRhs().size() == 1)
			{
				cnf.add(rule);
			}
		}
	}

	/**
	 * 将右侧全部转换为非终结符，并添加新的非终结符，新的规则
	 */
	private static void ConvertToNonTerRHS(RewriteRule rule, String type, CFG cnf)
	{
		ArrayList<String> rhs = new ArrayList<String>();
		for (String string : rule.getRhs())
		{
			if (cnf.isTerminal(string))
			{
				String newString = "$" + string + "$";
				cnf.addNonTerminal(newString);// 添加新的伪非终结符

				// 添加新的规则
				if (type.contains("P"))
				{
					cnf.add(new PRule(1.0, newString, string));
				}
				else
				{
					cnf.add(new RewriteRule(newString, string));
				}
				rhs.add(newString);
			}
			else
			{
				rhs.add(string);
			}
		}

		rule.setRhs(rhs);
	}

	/**
	 * 每次选择最右侧字符串的两个为新的规则的右侧字符串，以&联接两个非终结符，如此，方便在P2NF转回为CFG
	 */
	private static void reduceRHSNum(RewriteRule rule, String type, CFG cnf)
	{
		if (rule.getRhs().size() == 2)
		{
			cnf.add(rule);
			return;
		}

		List<String> list = rule.getRhs();
		int size = list.size();
		String str = list.get(size - 2) + "&" + list.get(size - 1);// 新规则的左侧

		// 最右侧的两个非终结符合成一个，并形成新的规则
		if (type.contains("P"))
		{
			cnf.add(new PRule(1.0, str, list.get(size - 2), list.get(size - 1)));
		}
		else
		{
			cnf.add(new RewriteRule(str, list.get(size - 2), list.get(size - 1)));
		}
		cnf.addNonTerminal(str);// 添加新的合成非终结符

		ArrayList<String> rhsList = new ArrayList<String>();
		rhsList.addAll(rule.getRhs().subList(0, rule.getRhs().size() - 2));
		rhsList.add(str);
		rule.setRhs(rhsList);

		// 递归，直到rhs的个数为2时
		reduceRHSNum(rule, type, cnf);
	}

	/**
	 * 消除单元规则
	 */
	private static void removeUnitProduction(String type, HashSet<String> posSet, CFG cnf)
	{
		HashSet<RewriteRule> deletePRuleSet = new HashSet<RewriteRule>();
		Set<String> nonterSet = cnf.getNonTerminalSet();
		for (String nonTer : cnf.getNonTerminalSet())
		{
			for (RewriteRule rule : cnf.getRuleBylhs(nonTer))
			{
				if (rule.getRhs().size() == 1) // 单元规则
				{
					String rhs = rule.getRhs().get(0);
					if (posSet.contains(rhs)) // 右部是词性
					{// 消除单元规则终止与POS层次
						continue;
					}
					if (nonterSet.contains(rhs))
					{
						deletePRuleSet.add(rule);
						removeUPAndAddNewRule(rule, type, posSet, cnf);
					}
				}
			}
		}
		DeletePRuleSet(deletePRuleSet, cnf);
	}

	private static void removeUPAndAddNewRule(RewriteRule rule, String type, HashSet<String> posSet, CFG cnf)
	{
		String lhs = rule.getLhs();
		String rhs = rule.getRhs().get(0);

		String[] lhs1 = lhs.split("@");
		if (lhs1.length >= 3)
		{
			return;// 如果单元规则迭代有3次以上，则返回
		}
		if (posSet.contains(rule.getRhs().get(0)))
		{
			cnf.add(rule);// 若该规则右侧为词性标注则直接添加
			return;
		}
		for (String lhs2 : lhs1)
		{
			if (lhs2.equals(rhs))
			{
				return;// 如果出现循环非终结符则返回
			}
		}
		for (RewriteRule rule1 : cnf.getRuleBylhs(rule.getRhs().get(0)))
		{
			RewriteRule rule2;
			if (type.contains("P"))
			{
				PRule prule1 = (PRule) rule1;
				PRule prule = (PRule) rule;

				rule2 = new PRule(prule.getProb() * prule1.getProb(), prule.getLhs() + "@" + prule1.getLhs(),
						prule1.getRhs());
			}
			else
			{
				rule2 = new RewriteRule(rule.getLhs() + "@" + rule1.getLhs(), rule1.getRhs());
			}
			if (rule1.getRhs().size() == 2 || !cnf.getNonTerminalSet().contains(rule1.getRhs().get(0)))
			{
				cnf.add(rule2);
			}
			else
			{
				removeUPAndAddNewRule(rule2, type, posSet, cnf);
			}
		}
	}

	private static void DeletePRuleSet(HashSet<RewriteRule> deletePRuleSet, CFG cnf)
	{
		for (RewriteRule rule : deletePRuleSet)
		{
			cnf.getRuleSet().remove(rule);
			cnf.getRuleBylhs(rule.getLhs()).remove(rule);
			cnf.getRuleByrhs(rule.getRhs()).remove(rule);
		}
	}

	/**
	 * 得到词性标注
	 */
	private static HashSet<String> getPOSSet(CFG cnf)
	{
		HashSet<String> posSet = new HashSet<String>();

		for (RewriteRule rule : cnf.getRuleSet())
		{
			if (rule.getRhs().size() == 1 && cnf.isTerminal(rule.getRhs().get(0)))
			{
				posSet.add(rule.getLhs());
			}
		}
		
		return posSet;
	}
}
