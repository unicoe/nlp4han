package org.nlp4han.coref.hobbs;

import java.util.List;

import com.lc.nlp4han.constituent.TreeNode;

/**
 * 过滤器接口
 * 
 * @author 杨智超
 *
 */
public abstract class CandidateFilter
{
	/**
	 * 进行过滤
	 * 
	 * @return
	 */
	public abstract List<TreeNode> filter();

	/**
	 * 设置待过滤结点
	 * 
	 * @param treeNodes 待过滤结点
	 */
	public abstract void setFilteredNodes(List<TreeNode> treeNodes);

	/**
	 * 设置过滤的参考条件
	 * 
	 * @param obj 过滤的参考条件信息
	 */
	public void setReferenceConditions(Object obj)
	{
		
	}
}
