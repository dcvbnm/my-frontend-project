package org.ershoupingtai.pojo.user;

// 对应 Collection 表的简化映射，用于拼装“我的收藏”视图数据。
public class CollectionEntity {
    private Long collectId;
    private Long userId;
    private Long goodsId;

    public Long getCollectId() {
        return collectId;
    }

    public void setCollectId(Long collectId) {
        this.collectId = collectId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getGoodsId() {
        return goodsId;
    }

    public void setGoodsId(Long goodsId) {
        this.goodsId = goodsId;
    }
}
