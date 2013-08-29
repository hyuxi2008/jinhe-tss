package com.jinhe.tss.cms.dao;

import java.io.File;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.springframework.stereotype.Repository;

import com.jinhe.tss.cms.CMSConstants;
import com.jinhe.tss.cms.entity.Article;
import com.jinhe.tss.cms.entity.Attachment;
import com.jinhe.tss.cms.entity.Channel;
import com.jinhe.tss.cms.helper.ArticleHelper;
import com.jinhe.tss.cms.helper.ArticleQueryCondition;
import com.jinhe.tss.framework.persistence.BaseDao;
import com.jinhe.tss.framework.persistence.pagequery.PageInfo;
import com.jinhe.tss.framework.persistence.pagequery.PaginationQueryByHQL;
import com.jinhe.tss.util.EasyUtils;

/**
 * Article的Dao层，负责处理Article相关的数据库操作
 */
@Repository("ArticleDao")
public class ArticleDao extends BaseDao<Article> implements IArticleDao {
    
    public ArticleDao() {
        super(Article.class);
    }
 
    public Article saveArticle(Article article) {
        return create(article);
    }

    public void deleteArticle(Article article) {
        Long articleId = article.getId();
        
        // 删除附件
        List<Attachment> attachments = getArticleAttachments(articleId); // 后台查找的新建文章时上传的附件列表
        for ( Attachment attachment : attachments ) {
            // 删除附件 TODO 如果是缩略图则还需删除原图片
            Channel site = article.getChannel().getSite();
			new File( ArticleHelper.getAttachUploadPath(site, attachment)[0] ).delete();
           
            // 删除老的附件信息
            super.delete(attachment);
        }
        
        // 删除文章生成的xml文件 （如果有的话）
        String pubUrl = article.getPubUrl();
        if ( !EasyUtils.isNullOrEmpty(pubUrl) ) {
            new File(pubUrl).delete(); 
        }
        
        super.delete(article);
    }
    
    public Integer getAttachmentIndex(Long articleId) {
        String hql = "select nvl(max(o.id.seqNo), 0) + 1 from Attachment o where o.articleId = ?";
        List<?> list = getEntities(hql, articleId);
        Integer nextSeqNo = (Integer) list.get(0);
        if (nextSeqNo == null) {
        	return 1;
        }
        return nextSeqNo + 1;
    }
    
    public Attachment getAttachment(Long articleId, Integer seqNo) {
        String hql = " from Attachment o where o.articleId = ? and o.seqNo = ?";
        List<?> list = getEntities(hql, articleId, seqNo);
        if( list.size() > 0 ) {
            Attachment attachment = (Attachment) list.get(0);
            attachment.setArticle(getEntity(attachment.getArticleId()));
            
			return attachment;
        }
        return null;
    }
 
	@SuppressWarnings("unchecked")
	public List<Attachment> getArticleAttachments(Long articleId) {
        List<Attachment> list = (List<Attachment>)getEntities("from Attachment o where o.articleId = ?", articleId);
		for ( Attachment attachment : list ) {
			attachment.setArticle(getEntity(articleId));
			attachment.setUploadName(attachment.getRelateDownloadUrl()); // 设置附件的下载地址
		}
		return list;
	}

	public List<?> getPublishedArticleByChannel(Long channelId) {
		String hql = "select a.id, a.pubUrl, a.issueDate " +
				" from Article a " +
				" where a.channel.id = ? and a.status = ? ";
        return getEntities(hql, channelId, CMSConstants.XML_STATUS );
	}

    @SuppressWarnings("unchecked")
	public List<Article> getExpireArticlePuburlList(Date now, Long channelId) {
        String hql = "from Article a " +
                " where a.channel.id = ? and a.status = ? and a.overdueDate <= ? ";
        
        // 需要过期的文章为”已发布“状态的文章。其他状态没必要设置为过期
        return (List<Article>) getEntities(hql, channelId, CMSConstants.XML_STATUS, now);
    }
 
    //* *****************************************            for page search         ********************************************
    
	public PageInfo getPageList(Long channelId, Integer pageNum, String...orderBy) {
	    String hql = "select a.id, a.title, a.author, a.issueDate, a.summary, "
				+ "  a.hitCount, a.creatorName, a.createTime, "
				+ "  a.status, a.channel, a.isTop, a.overdueDate"
				+ " from Article a "
				+ " where a.channel.id = :channelId ";
		
        ArticleQueryCondition condition = new ArticleQueryCondition();
        condition.setChannelId(channelId);
        condition.getPage().setPageNum(pageNum);
	        
        if(orderBy != null) {
            condition.getOrderByFields().addAll( Arrays.asList(orderBy) );
        } 
        else {
            condition.getOrderByFields().add(" a.createTime desc ");  //  默认按seqNo排序
        }
 
		PaginationQueryByHQL pageQuery = new PaginationQueryByHQL(em, hql, condition);
		return pageQuery.getResultList();
	}
	
	public PageInfo getSearchArticlePageList(ArticleQueryCondition condition) {
		String hql = "select a.id, a.title, a.author, a.issueDate, a.summary, "
				+ "  a.hitCount, a.creatorName, a.createTime, "
				+ "  a.status, a.channel, a.isTop, a.overdueDate"
                + " from Article a, Temp t"
				+ " where a.channel.id = t.id "
				+ " ${author} ${status} ${title} ${createTime}" ;
        
        String orderField = condition.getOrderField();
		String orderBy = orderField == null ? null : "a." + orderField;
        if( orderBy != null && CMSConstants.TRUE.equals(condition.getIsDesc()) ) {
            orderBy += " desc ";
        }
        
        if(orderBy == null) {
            orderBy = " a.createTime desc "; // 默认按文章的创建时间排序
        }
        condition.getOrderByFields().add(orderBy);
 
		PaginationQueryByHQL pageQuery = new PaginationQueryByHQL(em, hql, condition);
		return pageQuery.getResultList();
	}
 

	//* *****************************************  for portlet  ********************************************
     
    public PageInfo getChannelPageArticleList(ArticleQueryCondition condition) {
        String hql = "select a.id, a.title, a.author, a.summary, a.issueDate, a.createTime, a.hitCount, a.isTop "
                + " from Article a"
                + " where 1=1 ${channelId} ${status} "
                + " order by a.isTop desc, a.createTime desc";
 
        PaginationQueryByHQL pageQuery = new PaginationQueryByHQL(em, hql, condition);
        return pageQuery.getResultList();
    }
    
    public PageInfo getArticlesByChannelIds(ArticleQueryCondition condition) {
        insertIds2TempTable(condition.getChannelIds());
        condition.setChannelIds(null);
 
        String hql = "select a.id, a.title, a.author, a.summary, a.issueDate, a.createTime, a.hitCount, a.isTop"
                    + " from Article a, Temp t "
                    + " where a.channel.id = t.id ${status} ${createTime} "
                    + " order by a.isTop desc, a.createTime desc";
        
        PaginationQueryByHQL pageQuery = new PaginationQueryByHQL(em, hql, condition);
        return pageQuery.getResultList();
    }
}